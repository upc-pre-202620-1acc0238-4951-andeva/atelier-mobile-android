package com.andeva.atelier.mobile.features.workorders.presentation.open

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.AddWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.application.FindDraftWorkOrderForVehicleUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetBranchMechanicsUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.OpenWorkOrderUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateWorkOrderUseCase
import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.presentation.invalidWorkOrderField
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * US12: abrir la orden con kilometraje, combustible y motivo, y elegir las faenas iniciales. Todo en línea (no se
 * encola). Si el vehículo ya tiene una orden en borrador (llegó por una cita, H10) se completa esa en vez de crear otra.
 */
@HiltViewModel
class OpenWorkOrderViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val openWorkOrder: OpenWorkOrderUseCase,
    private val updateWorkOrder: UpdateWorkOrderUseCase,
    private val addTask: AddWorkOrderTaskUseCase,
    private val findDraft: FindDraftWorkOrderForVehicleUseCase,
    private val getServices: GetServiceCatalogUseCase,
    private val getMechanics: GetBranchMechanicsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: String = savedStateHandle["vehicleId"] ?: ""
    private val customerId: String = savedStateHandle["customerId"] ?: ""

    private val _uiState = MutableStateFlow(OpenWorkOrderUiState(workOrderId = savedStateHandle["workOrderId"]))
    val uiState: StateFlow<OpenWorkOrderUiState> = _uiState.asStateFlow()

    private var draftChecked = false

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canCreate = session?.has(Permission.WORK_ORDERS_CREATE) == true,
                        canUpdate = session?.has(Permission.WORK_ORDERS_UPDATE) == true
                    )
                }
                val branchId = session?.branchId
                if (!draftChecked && branchId != null) {
                    draftChecked = true
                    lookForDraft(branchId)
                }
            }
        }
        loadCatalog()
    }

    /** Si la orden no vino en la ruta, busca un borrador del vehículo para no crear otra (evita el 409). */
    private fun lookForDraft(branchId: String) {
        if (_uiState.value.workOrderId != null) return
        viewModelScope.launch {
            findDraft(branchId, vehicleId).getOrNull()?.let { draft ->
                _uiState.update { if (it.workOrderId == null && !it.orderSaved) it.copy(workOrderId = draft.id) else it }
            }
        }
    }

    /** Servicios y mecánicos para elegir. Sin permiso de lectura el backend responde vacío y la orden va sin faenas. */
    fun loadCatalog() {
        _uiState.update { it.copy(isLoadingCatalog = true, error = null) }
        viewModelScope.launch {
            val services = getServices()
            val mechanics = getMechanics()
            _uiState.update {
                it.copy(
                    isLoadingCatalog = false,
                    services = services.getOrDefault(emptyList()),
                    mechanics = mechanics.getOrDefault(emptyList()),
                    error = (services.exceptionOrNull() ?: mechanics.exceptionOrNull())?.toWorkOrderUiText()
                )
            }
        }
    }

    private fun edit(change: (OpenWorkOrderUiState) -> OpenWorkOrderUiState) = _uiState.update {
        if (it.formLocked) it else change(it).copy(error = null, invalidField = null)
    }

    fun onMileageChange(value: String) = edit { it.copy(mileage = value.filter(Char::isDigit)) }
    fun onFuelChange(value: FuelLevel) = edit { it.copy(fuel = value) }
    fun onReasonChange(value: String) = edit { it.copy(reason = value) }

    fun onToggleService(serviceId: String) = _uiState.update {
        // Las faenas ya agregadas a la orden no se quitan desde aquí.
        if (it.isSaving || serviceId in it.addedServiceIds) it
        else it.copy(
            selectedServiceIds = if (serviceId in it.selectedServiceIds) it.selectedServiceIds - serviceId
            else it.selectedServiceIds + serviceId,
            error = null
        )
    }

    fun onMechanicChange(membershipId: String?) = _uiState.update {
        if (it.isSaving) it else it.copy(selectedMechanicId = membershipId, error = null)
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val mileage = state.mileage.toIntOrNull()
        if (mileage == null) {
            fail(InvalidWorkOrderInputException("mileageIn"))
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null, invalidField = null) }
        viewModelScope.launch {
            var orderId = state.workOrderId
            if (!state.orderSaved) {
                val summary = "Combustible: ${state.fuel?.summaryLabel}. ${state.reason.trim()}"
                val saved = if (orderId != null) updateWorkOrder(orderId, mileage, summary)
                else openWorkOrder(NewWorkOrder(vehicleId, customerId, mileage, summary))
                orderId = saved.getOrElse { return@launch fail(it) }.id
                _uiState.update { it.copy(workOrderId = orderId, orderSaved = true) }
            }
            for (service in state.pendingServices) {
                val task = NewWorkOrderTask(service.id, service.name, state.selectedMechanicId)
                addTask(orderId!!, task)
                    .onSuccess { _uiState.update { it.copy(addedServiceIds = it.addedServiceIds + service.id) } }
                    .onFailure { return@launch fail(it) }
            }
            _uiState.update { it.copy(isSaving = false, doneWorkOrderId = orderId) }
        }
    }

    private fun fail(error: Throwable) = _uiState.update {
        it.copy(isSaving = false, error = error.toWorkOrderUiText(), invalidField = error.invalidWorkOrderField())
    }
}
