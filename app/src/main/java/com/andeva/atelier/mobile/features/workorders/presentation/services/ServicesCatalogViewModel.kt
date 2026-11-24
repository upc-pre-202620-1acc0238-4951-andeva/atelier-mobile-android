package com.andeva.atelier.mobile.features.workorders.presentation.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.CreateServiceUseCase
import com.andeva.atelier.mobile.features.workorders.application.DeleteServiceUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateServiceUseCase
import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.invalidWorkOrderField
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Gestión del catálogo de servicios estándar del taller (`GET/POST/PUT/DELETE /api/v1/services`). */
@HiltViewModel
class ServicesCatalogViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val refreshServices: RefreshServiceCatalogUseCase,
    private val createService: CreateServiceUseCase,
    private val updateService: UpdateServiceUseCase,
    private val deleteService: DeleteServiceUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesCatalogUiState())
    val uiState: StateFlow<ServicesCatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canManage = session?.has(Permission.SERVICES_MANAGE) == true
                val canRead = session?.has(Permission.SERVICES_READ) == true || canManage
                _uiState.update { it.copy(canRead = canRead, canManage = canManage) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            refreshServices()
                .onSuccess { list ->
                    _uiState.update { it.copy(services = list, isLoading = false, error = null) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.toWorkOrderUiText()) }
                }
        }
    }

    fun onSearchQueryChange(value: String) = _uiState.update { it.copy(searchQuery = value) }

    fun onOpenCreate() {
        val state = _uiState.value
        if (!state.canCreate) return
        _uiState.update {
            it.copy(
                form = ServiceEditorForm(),
                notice = null,
                error = null,
                invalidField = null
            )
        }
    }

    fun onOpenEdit(service: ServiceOption) {
        val state = _uiState.value
        if (!state.canManage || state.isSaving) return
        _uiState.update {
            it.copy(
                form = ServiceEditorForm(
                    serviceId = service.id,
                    name = service.name,
                    basePrice = service.basePrice?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
                    estimatedMinutes = service.estimatedMinutes?.toString().orEmpty()
                ),
                notice = null,
                error = null,
                invalidField = null
            )
        }
    }

    fun onDismissForm() = _uiState.update {
        if (it.isSaving) it else it.copy(form = null, error = null, invalidField = null)
    }

    fun onNameChange(value: String) = editForm { it.copy(name = value) }

    fun onBasePriceChange(value: String) = editForm { it.copy(basePrice = value) }

    fun onEstimatedMinutesChange(value: String) = editForm { it.copy(estimatedMinutes = value.filter(Char::isDigit)) }

    private fun editForm(change: (ServiceEditorForm) -> ServiceEditorForm) = _uiState.update {
        val current = it.form
        if (current == null || it.isSaving) it else it.copy(form = change(current), error = null, invalidField = null)
    }

    fun onConfirmForm() {
        val state = _uiState.value
        val form = state.form ?: return
        if (!state.canManage || state.isSaving || !state.isOnline) return

        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 150) {
            fail(InvalidWorkOrderInputException("name"))
            return
        }
        val parsedPrice = form.basePrice.trim().replace(',', '.').toBigDecimalOrNull()
        if (parsedPrice == null || parsedPrice <= BigDecimal.ZERO) {
            fail(InvalidWorkOrderInputException("basePrice"))
            return
        }
        val parsedMinutes = form.estimatedMinutes.trim().toIntOrNull()
        if (parsedMinutes == null || parsedMinutes <= 0) {
            fail(InvalidWorkOrderInputException("estimatedMinutes"))
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null, invalidField = null) }
        viewModelScope.launch {
            val result = if (form.serviceId == null) {
                createService(trimmedName, parsedPrice, parsedMinutes)
            } else {
                updateService(form.serviceId, trimmedName, parsedPrice, parsedMinutes)
            }
            result
                .onSuccess { saved ->
                    val noticeRes = if (form.serviceId == null) {
                        R.string.services_created_notice
                    } else {
                        R.string.services_updated_notice
                    }
                    _uiState.update { current ->
                        val updatedList = (current.services.filterNot { it.id == saved.id } + saved)
                            .sortedBy { it.name.lowercase() }
                        current.copy(
                            services = updatedList,
                            isSaving = false,
                            form = null,
                            notice = UiText.Resource(noticeRes),
                            error = null,
                            invalidField = null
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    fun onOpenDelete(service: ServiceOption) {
        val state = _uiState.value
        if (!state.canManage || state.isSaving) return
        _uiState.update { it.copy(deletingService = service, form = null, error = null, notice = null) }
    }

    fun onDismissDelete() = _uiState.update {
        if (it.isSaving) it else it.copy(deletingService = null, error = null)
    }

    fun onConfirmDelete() {
        val state = _uiState.value
        val target = state.deletingService ?: return
        if (!state.canManage || state.isSaving || !state.isOnline) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            deleteService(target.id)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            services = current.services.filterNot { it.id == target.id },
                            isSaving = false,
                            deletingService = null,
                            notice = UiText.Resource(R.string.services_deleted_notice),
                            error = null
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    private fun fail(error: Throwable) = _uiState.update {
        it.copy(
            isSaving = false,
            error = error.toWorkOrderUiText(),
            invalidField = error.invalidWorkOrderField()
        )
    }
}
