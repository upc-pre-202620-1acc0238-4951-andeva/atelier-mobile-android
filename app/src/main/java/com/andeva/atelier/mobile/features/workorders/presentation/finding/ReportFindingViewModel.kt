package com.andeva.atelier.mobile.features.workorders.presentation.finding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObservePendingProposalsUseCase
import com.andeva.atelier.mobile.features.workorders.application.SubmitProposalUseCase
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.presentation.invalidWorkOrderField
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * US14: reportar un hallazgo en foso con severidad, descripción y foto probatoria. Funciona sin red: la propuesta se
 * encola y el backend avisa a supervisión al sincronizar; mientras tanto aparece como pendiente de envío.
 */
@HiltViewModel
class ReportFindingViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val submitProposal: SubmitProposalUseCase,
    observePending: ObservePendingProposalsUseCase,
    private val getServices: GetServiceCatalogUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(ReportFindingUiState())
    val uiState: StateFlow<ReportFindingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canCreate = session?.has(Permission.PROPOSALS_CREATE) == true) }
            }
        }
        viewModelScope.launch {
            observePending(workOrderId).collect { list -> _uiState.update { it.copy(pending = list) } }
        }
        loadServices()
    }

    /** Servicios sugeridos (opcional). Sin permiso de lectura llega vacío y el hallazgo se envía sin sugerencia. */
    fun loadServices() {
        _uiState.update { it.copy(isLoadingServices = true) }
        viewModelScope.launch {
            getServices()
                .onSuccess { list -> _uiState.update { it.copy(isLoadingServices = false, services = list) } }
                .onFailure { e -> _uiState.update { it.copy(isLoadingServices = false, error = e.toWorkOrderUiText()) } }
        }
    }

    private fun edit(change: (ReportFindingUiState) -> ReportFindingUiState) = _uiState.update {
        if (it.isSaving) it else change(it).copy(error = null, invalidField = null, saved = false)
    }

    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }
    fun onSeverityChange(value: ProposalSeverity) = edit { if (value.isSubmittable) it.copy(severity = value) else it }
    fun onServiceChange(serviceId: String?) = edit { it.copy(suggestedServiceId = serviceId) }

    /** Foto tomada: reemplaza a la anterior, que se descarta. Se comprime y encola al enviar. */
    fun onPhotoCaptured(file: File) {
        val previous = _uiState.value.photo
        if (previous != null && previous != file) previous.delete()
        edit { it.copy(photo = file) }
    }

    fun onCameraDenied() = _uiState.update { it.copy(error = UiText.Resource(R.string.gallery_camera_denied)) }

    fun onSubmit() {
        val state = _uiState.value
        val severity = state.severity
        val photo = state.photo
        if (!state.canSubmit || severity == null) return
        _uiState.update { it.copy(isSaving = true, error = null, invalidField = null, saved = false) }
        viewModelScope.launch {
            submitProposal(workOrderId, state.description.trim(), severity, photo, state.suggestedServiceId)
                .onSuccess {
                    photo?.delete()
                    _uiState.update {
                        it.copy(isSaving = false, saved = true, description = "", severity = null, photo = null, suggestedServiceId = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.toWorkOrderUiText(), invalidField = e.invalidWorkOrderField())
                    }
                }
        }
    }
}
