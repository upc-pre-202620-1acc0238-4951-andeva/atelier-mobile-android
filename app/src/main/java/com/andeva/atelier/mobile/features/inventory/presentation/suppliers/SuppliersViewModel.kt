package com.andeva.atelier.mobile.features.inventory.presentation.suppliers

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.CreateSupplierUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetSuppliersUseCase
import com.andeva.atelier.mobile.features.inventory.application.UpdateSupplierUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuppliersUiState(
    val query: String = "",
    val suppliers: List<Supplier> = emptyList(),
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canManage: Boolean = true,
    val bannerMessage: UiText? = null,
    val sheetOpen: Boolean = false,
    val editingSupplier: Supplier? = null,
    val businessName: String = "",
    val taxId: String = "",
    val contactName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val active: Boolean = true,
    @StringRes val nameError: Int? = null,
    @StringRes val taxIdError: Int? = null,
    @StringRes val emailError: Int? = null,
    val submitError: UiText? = null,
    val isSaving: Boolean = false,
    val hapticEvent: Int = 0,
    val hapticIsError: Boolean = false
) {
    val isEditing: Boolean get() = editingSupplier != null
}

/** Catálogo y alta/edición de proveedores homologados (`inventory:suppliers:manage`). */
@HiltViewModel
class SuppliersViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getSuppliers: GetSuppliersUseCase,
    private val createSupplier: CreateSupplierUseCase,
    private val updateSupplier: UpdateSupplierUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canManage = session?.has(Permission.SUPPLIERS_MANAGE) == true) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && !_uiState.value.isOnline && _uiState.value.error != null
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() = viewModelScope.launch {
        searchQuery.debounce(SEARCH_DEBOUNCE_MS).collectLatest { q ->
            loadSuppliers(q)
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchQuery.value = value
    }

    fun refresh() {
        loadSuppliers(_uiState.value.query)
    }

    private fun loadSuppliers(query: String) {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            getSuppliers(search = query.trim().ifEmpty { null })
                .onSuccess { list -> _uiState.update { it.copy(suppliers = list, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUiText()) } }
        }
    }

    fun onOpenCreate() {
        _uiState.update {
            it.copy(
                sheetOpen = true,
                editingSupplier = null,
                businessName = "",
                taxId = "",
                contactName = "",
                phone = "",
                email = "",
                address = "",
                active = true,
                nameError = null,
                taxIdError = null,
                emailError = null,
                submitError = null
            )
        }
    }

    fun onOpenEdit(supplier: Supplier) {
        _uiState.update {
            it.copy(
                sheetOpen = true,
                editingSupplier = supplier,
                businessName = supplier.businessName,
                taxId = supplier.taxId,
                contactName = supplier.contactName.orEmpty(),
                phone = supplier.phone.orEmpty(),
                email = supplier.email.orEmpty(),
                address = supplier.address.orEmpty(),
                active = supplier.active,
                nameError = null,
                taxIdError = null,
                emailError = null,
                submitError = null
            )
        }
    }

    fun onDismissSheet() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(sheetOpen = false, submitError = null) }
    }

    fun onBusinessNameChange(v: String) = _uiState.update { it.copy(businessName = v, nameError = null, submitError = null) }
    fun onTaxIdChange(v: String) = _uiState.update { it.copy(taxId = v, taxIdError = null, submitError = null) }
    fun onContactNameChange(v: String) = _uiState.update { it.copy(contactName = v, submitError = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v, submitError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null, submitError = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v, submitError = null) }
    fun onActiveChange(v: Boolean) = _uiState.update { it.copy(active = v, submitError = null) }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || !state.isOnline) return
        val nameErr = if (!InventoryValidation.isNonEmptyText(state.businessName)) R.string.inv_seg2_supplier_err_name else null
        val taxErr = if (!state.isEditing && !InventoryValidation.isTaxId(state.taxId)) R.string.inv_seg2_supplier_err_tax_id else null
        val emailErr = if (!InventoryValidation.isOptionalEmail(state.email)) R.string.inv_seg2_supplier_err_email else null
        if (nameErr != null || taxErr != null || emailErr != null) {
            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    taxIdError = taxErr,
                    emailError = emailErr,
                    hapticEvent = it.hapticEvent + 1,
                    hapticIsError = true
                )
            }
            return
        }
        _uiState.update { it.copy(isSaving = true, submitError = null) }
        viewModelScope.launch {
            val editing = state.editingSupplier
            val result = if (editing == null) {
                createSupplier(
                    businessName = state.businessName,
                    taxId = state.taxId,
                    contactName = state.contactName,
                    phone = state.phone,
                    email = state.email,
                    address = state.address
                )
            } else {
                updateSupplier(
                    id = editing.id,
                    businessName = state.businessName,
                    contactName = state.contactName,
                    phone = state.phone,
                    email = state.email,
                    address = state.address,
                    active = state.active
                )
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            sheetOpen = false,
                            bannerMessage = UiText.Resource(R.string.inv_seg2_supplier_saved),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                    refresh()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            submitError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
