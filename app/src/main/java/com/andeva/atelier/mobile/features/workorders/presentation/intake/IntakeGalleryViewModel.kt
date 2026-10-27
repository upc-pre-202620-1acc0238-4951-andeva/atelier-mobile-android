package com.andeva.atelier.mobile.features.workorders.presentation.intake

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.AttachIntakePhotoUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObservePendingIntakePhotosUseCase
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
 * US13: galería pericial de recepción. Las fotos van siempre por la cola (con o sin red) y aparecen como "en cola"
 * hasta que el backend las registra; las ya registradas salen del detalle de la orden (E2).
 */
@HiltViewModel
class IntakeGalleryViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val attachPhoto: AttachIntakePhotoUseCase,
    observePending: ObservePendingIntakePhotosUseCase,
    private val getDetail: GetWorkOrderDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(IntakeGalleryUiState())
    val uiState: StateFlow<IntakeGalleryUiState> = _uiState.asStateFlow()

    private var detailRequested = false

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canRead = session?.has(Permission.WORK_ORDERS_READ) == true
                _uiState.update {
                    it.copy(canRead = canRead, canUpload = session?.has(Permission.INSPECTIONS_UPLOAD_PHOTOS) == true)
                }
                // La sesión llega de forma asíncrona: el detalle se pide al conocer el permiso, una sola vez.
                if (!detailRequested) {
                    detailRequested = true
                    if (canRead) refresh() else _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
        viewModelScope.launch {
            var previous = 0
            observePending(workOrderId).collect { pending ->
                _uiState.update { it.copy(pending = pending) }
                // Una foto salió de la cola: ya está registrada y hay que traerla del detalle.
                if (pending.size < previous && _uiState.value.canRead) refresh()
                previous = pending.size
            }
        }
    }

    /** `GET /work-orders/{id}`: fotos ya registradas. Sin red se sigue con lo que hay y las fotos en cola. */
    fun refresh() {
        viewModelScope.launch {
            getDetail(workOrderId)
                .onSuccess { detail -> _uiState.update { it.copy(isLoading = false, uploaded = detail.intakeImages, error = null) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toWorkOrderUiText()) } }
        }
    }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value, error = null) }

    /** La foto ya fue tomada: se comprime, se guarda y se encola. Funciona sin red. */
    fun onPhotoCaptured(angle: InspectionAngle, photo: File) {
        val state = _uiState.value
        if (!state.canUpload || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            attachPhoto(workOrderId, photo, InspectionAngle.describe(angle, state.note))
                .onSuccess { _uiState.update { it.copy(isSaving = false, note = "") } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.toWorkOrderUiText()) } }
            photo.delete()
        }
    }

    fun onCameraDenied() = _uiState.update { it.copy(error = UiText.Resource(R.string.gallery_camera_denied)) }
}
