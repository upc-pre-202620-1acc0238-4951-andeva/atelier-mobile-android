package com.andeva.atelier.mobile.features.reception.presentation.affiliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.AffiliateCustomerUseCase
import com.andeva.atelier.mobile.features.reception.application.LookupAffiliationUseCase
import com.andeva.atelier.mobile.features.reception.presentation.invalidReceptionField
import com.andeva.atelier.mobile.features.reception.presentation.toReceptionUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AffiliationViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val lookupAffiliation: LookupAffiliationUseCase,
    private val affiliateCustomer: AffiliateCustomerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AffiliationUiState())
    val uiState: StateFlow<AffiliationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canLookup = session?.has(Permission.CUSTOMERS_READ) == true,
                        canAffiliate = session?.has(Permission.CUSTOMERS_CREATE) == true
                    )
                }
            }
        }
    }

    /** Cambiar el correo descarta el resultado anterior: el siguiente paso depende de ese correo. */
    fun onEmailChange(value: String) = _uiState.update {
        if (it.isBusy) it else it.copy(email = value, lookup = null, affiliated = null, error = null, invalidField = null)
    }

    fun onTaxIdChange(value: String) = _uiState.update {
        if (it.isBusy) it else it.copy(taxId = value, error = null, invalidField = null)
    }

    fun onPhoneChange(value: String) = _uiState.update {
        if (it.isBusy) it else it.copy(phone = value, error = null, invalidField = null)
    }

    /** `GET /customers/affiliation-lookup` (US44): `nextStep` decide qué se muestra. En línea. */
    fun onSearch() {
        val state = _uiState.value
        if (!state.canSearch) return
        _uiState.update { it.copy(isSearching = true, lookup = null, affiliated = null, error = null, invalidField = null) }
        viewModelScope.launch {
            lookupAffiliation(state.email.trim())
                .onSuccess { lookup -> _uiState.update { it.copy(isSearching = false, lookup = lookup) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSearching = false, error = e.toReceptionUiText(), invalidField = e.invalidReceptionField())
                    }
                }
        }
    }

    /** `POST /customers/affiliations` (US44 E1): `taxId` obligatorio. En línea, no se encola. */
    fun onAffiliate() {
        val state = _uiState.value
        if (!state.canSubmitAffiliation) return
        _uiState.update { it.copy(isAffiliating = true, error = null, invalidField = null) }
        viewModelScope.launch {
            affiliateCustomer(state.email.trim(), state.taxId.trim(), state.phone.trim().ifBlank { null })
                .onSuccess { customer -> _uiState.update { it.copy(isAffiliating = false, affiliated = customer) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isAffiliating = false, error = e.toReceptionUiText(), invalidField = e.invalidReceptionField())
                    }
                }
        }
    }
}
