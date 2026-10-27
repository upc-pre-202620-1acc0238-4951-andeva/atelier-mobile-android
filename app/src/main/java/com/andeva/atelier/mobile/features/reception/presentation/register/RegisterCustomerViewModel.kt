package com.andeva.atelier.mobile.features.reception.presentation.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.RegisterCompanyCustomerUseCase
import com.andeva.atelier.mobile.features.reception.application.RegisterIndividualCustomerUseCase
import com.andeva.atelier.mobile.features.reception.application.RegisterVehicleUseCase
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.NewCompanyCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewIndividualCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewVehicle
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import com.andeva.atelier.mobile.features.reception.presentation.invalidReceptionField
import com.andeva.atelier.mobile.features.reception.presentation.toReceptionUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Alta presencial de cliente y vehículo (US11 E2, US44 E2). En línea: sin red falla y no se encola. */
@HiltViewModel
class RegisterCustomerViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val registerIndividual: RegisterIndividualCustomerUseCase,
    private val registerCompany: RegisterCompanyCustomerUseCase,
    private val registerVehicle: RegisterVehicleUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterCustomerUiState(email = savedStateHandle["email"] ?: ""))
    val uiState: StateFlow<RegisterCustomerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val allowed = session?.has(Permission.CUSTOMERS_CREATE) == true &&
                    session.has(Permission.VEHICLES_CREATE)
                _uiState.update { it.copy(canRegister = allowed) }
            }
        }
    }

    /** Edita un campo mientras no se esté guardando; limpia el error anterior. */
    private fun edit(change: (RegisterCustomerUiState) -> RegisterCustomerUiState) = _uiState.update {
        if (it.isSaving) it else change(it).copy(error = null, invalidField = null)
    }

    /** El tipo de cliente no cambia una vez creado el cliente. */
    fun onKindChange(kind: CustomerType) = edit { if (it.customerLocked) it else it.copy(kind = kind) }

    fun onFirstNameChange(value: String) = edit { if (it.customerLocked) it else it.copy(firstName = value) }
    fun onLastNameChange(value: String) = edit { if (it.customerLocked) it else it.copy(lastName = value) }
    fun onCompanyNameChange(value: String) = edit { if (it.customerLocked) it else it.copy(companyName = value) }
    fun onTaxIdChange(value: String) = edit { if (it.customerLocked) it else it.copy(taxId = value) }
    fun onEmailChange(value: String) = edit { if (it.customerLocked) it else it.copy(email = value) }
    fun onPhoneChange(value: String) = edit { if (it.customerLocked) it else it.copy(phone = value) }
    fun onPlateChange(value: String) = edit { it.copy(plate = value) }
    fun onBrandChange(value: String) = edit { it.copy(brand = value) }
    fun onModelChange(value: String) = edit { it.copy(model = value) }
    fun onEngineTypeChange(value: EngineType) = edit { it.copy(engineType = value) }
    fun onVinChange(value: String) = edit { it.copy(vin = value) }
    fun onYearChange(value: String) = edit { it.copy(year = value.filter(Char::isDigit)) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        // Se valida antes de crear al cliente: un año inválido no debe dejarlo guardado sin vehículo.
        val year = state.year.toIntOrNull()
        if (state.year.isNotBlank() && !ReceptionValidation.isOptionalYear(year)) {
            fail(InvalidReceptionInputException("year"))
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null, invalidField = null) }
        viewModelScope.launch {
            val customerId = state.registeredCustomerId ?: createCustomer(state) ?: return@launch
            registerVehicle(
                NewVehicle(
                    plate = state.plate,
                    brand = state.brand.trim(),
                    model = state.model.trim(),
                    engineType = state.engineType,
                    customerId = customerId,
                    vin = state.vin.trim().ifBlank { null },
                    year = year
                )
            )
                .onSuccess { _uiState.update { it.copy(isSaving = false, done = true) } }
                .onFailure(::fail)
        }
    }

    /** Crea al cliente y lo recuerda en el estado; null si falló (el error ya quedó en pantalla). */
    private suspend fun createCustomer(state: RegisterCustomerUiState): String? {
        val email = state.email.trim().ifBlank { null }
        val phone = state.phone.trim().ifBlank { null }
        val result = when (state.kind) {
            CustomerType.INDIVIDUAL -> registerIndividual(
                NewIndividualCustomer(state.firstName.trim(), state.lastName.trim(), state.taxId.trim(), email, phone)
            )
            CustomerType.COMPANY -> registerCompany(
                NewCompanyCustomer(state.companyName.trim(), state.taxId.trim(), email, phone)
            )
        }
        return result
            .onSuccess { customer -> _uiState.update { it.copy(registeredCustomerId = customer.id) } }
            .onFailure(::fail)
            .getOrNull()
            ?.id
    }

    private fun fail(error: Throwable) = _uiState.update {
        it.copy(isSaving = false, error = error.toReceptionUiText(), invalidField = error.invalidReceptionField())
    }
}
