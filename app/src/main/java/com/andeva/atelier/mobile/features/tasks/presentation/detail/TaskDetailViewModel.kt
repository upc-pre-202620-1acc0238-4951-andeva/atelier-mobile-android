package com.andeva.atelier.mobile.features.tasks.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.DiscardPendingOperationUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.main.application.RetryPendingOperationUseCase
import com.andeva.atelier.mobile.features.tasks.application.AddPartToTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.ApproveSuspensionUseCase
import com.andeva.atelier.mobile.features.tasks.application.AttachTaskEvidenceUseCase
import com.andeva.atelier.mobile.features.tasks.application.CompleteTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.GetTaskDetailUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObservePendingEvidenceUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.PauseTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.RejectSuspensionUseCase
import com.andeva.atelier.mobile.features.tasks.application.RemovePartFromTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.ReopenTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.RequestSuspensionUseCase
import com.andeva.atelier.mobile.features.tasks.application.ResumeSuspendedTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SearchPartsUseCase
import com.andeva.atelier.mobile.features.tasks.application.StartTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SuggestLaborHoursUseCase
import com.andeva.atelier.mobile.features.tasks.application.UpdateTaskPartQuantityUseCase
import com.andeva.atelier.mobile.features.tasks.domain.EvidenceKind
import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.LaborTime
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PausedElapsedStore
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.PendingEvidence
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.invalidTaskField
import com.andeva.atelier.mobile.features.tasks.presentation.toTaskUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import javax.inject.Inject

/**
 * US27–US30, US23, US33: detalle de tarea con cronómetro tabular, pausa tipificada, evidencias offline (US28),
 * suspensiones mecánico/supervisor (US29), cierre con horas sugeridas, reapertura y gestión FIFO de repuestos.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeTask: ObserveTaskUseCase,
    observePendingEvidence: ObservePendingEvidenceUseCase,
    private val getDetail: GetTaskDetailUseCase,
    private val startTask: StartTaskUseCase,
    private val pauseTask: PauseTaskUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val reopenTask: ReopenTaskUseCase,
    private val suggestLaborHours: SuggestLaborHoursUseCase,
    private val attachEvidence: AttachTaskEvidenceUseCase,
    private val requestSuspension: RequestSuspensionUseCase,
    private val approveSuspension: ApproveSuspensionUseCase,
    private val rejectSuspension: RejectSuspensionUseCase,
    private val resumeSuspendedTask: ResumeSuspendedTaskUseCase,
    private val searchParts: SearchPartsUseCase,
    private val addPart: AddPartToTaskUseCase,
    private val updatePartQuantity: UpdateTaskPartQuantityUseCase,
    private val removePart: RemovePartFromTaskUseCase,
    private val pausedElapsed: PausedElapsedStore,
    private val clock: Clock,
    private val connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle,
    private val retryPendingOperation: RetryPendingOperationUseCase? = null,
    private val discardPendingOperation: DiscardPendingOperationUseCase? = null
) : ViewModel() {

    private val taskId = TaskId(savedStateHandle["taskId"] ?: "")
    private val routeWorkOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState(fallbackWorkOrderId = routeWorkOrderId))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canOpenObd = session?.let { s ->
                    s.canUseTelemetry() || s.canReadTelemetry() || s.canUseAiDiagnostics() || s.canReadAiReports() ||
                        s.has(Permission.DEVICES_READ) || s.has(Permission.DEVICES_REGISTER) || s.has(Permission.PREDICTIVE_ALERTS_READ)
                } == true
                _uiState.update {
                    it.copy(
                        canTrackTime = session?.has(Permission.TASKS_TRACK_TIME) == true,
                        canComplete = session?.has(Permission.TASKS_COMPLETE) == true,
                        canChargeParts = session?.has(Permission.TASKS_CHARGE_PARTS) == true ||
                            session?.has(Permission.WORK_ORDERS_UPDATE) == true,
                        canEditParts = session?.has(Permission.WORK_ORDERS_UPDATE) == true,
                        canReadPartsCatalog = session?.has(Permission.PARTS_READ) == true,
                        canUploadPhotos = session?.has(Permission.TASKS_UPLOAD_PHOTOS) == true,
                        canRequestHold = session?.has(Permission.TASKS_HOLD_REQUEST) == true,
                        canValidateHold = session?.has(Permission.TASKS_HOLD_VALIDATE) == true,
                        canReportFinding = session?.has(Permission.PROPOSALS_CREATE) == true,
                        canOpenObd = canOpenObd,
                        canReopenTask = session?.has(Permission.WORK_ORDERS_UPDATE) == true
                    )
                }
            }
        }
        viewModelScope.launch {
            observeTask(taskId).collect { task -> _uiState.update { it.copy(task = task) } }
        }
        viewModelScope.launch {
            var previousPendingIds = emptySet<String>()
            observePendingEvidence(taskId).collect { pending ->
                val currentIds = pending.map { it.operationId }.toSet()
                val syncedSome = previousPendingIds.any { it !in currentIds }
                previousPendingIds = currentIds
                _uiState.update { it.copy(pendingEvidence = pending) }
                if (syncedSome && _uiState.value.isOnline) {
                    getDetail(taskId).onSuccess { detail ->
                        _uiState.update { it.copy(detail = detail) }
                    }
                }
            }
        }
        // D3: si la pantalla se abrió sin red, al volver la conexión se carga el detalle (repuestos incluidos).
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
                if (online && _uiState.value.detail == null && !_uiState.value.isLoading) load()
            }
        }
        load()
    }

    /** `GET /tasks/{id}`: servicio, repuestos imputados y evidencias. Sin red se sigue con la caché. */
    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getDetail(taskId)
                .onSuccess { detail -> _uiState.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toTaskUiText()) } }
            syncPausedElapsed()
        }
    }

    /** D1: recupera el tiempo guardado al pausar; fuera de una pausa no hay nada que mostrar y se limpia. */
    private suspend fun syncPausedElapsed() {
        val paused = _uiState.value.status == TaskStatus.ON_HOLD
        val seconds = if (paused) pausedElapsed.get(taskId) else null
        if (!paused) pausedElapsed.clear(taskId)
        _uiState.update { it.copy(pausedElapsedSeconds = seconds) }
    }

    /** US27 E1 y E5: pasa a `IN_PROGRESS`; el cronómetro cuenta desde `startedAt` menos lo pausado. */
    fun onStart() {
        if (!_uiState.value.canStart) return
        run(TaskAction.START) { startTask(taskId) }
    }

    fun onPauseClick() {
        if (!_uiState.value.canPause) return
        _uiState.update { it.copy(pauseForm = PauseForm(), notice = null, error = null) }
    }

    fun onPauseDismiss() = _uiState.update { it.copy(pauseForm = null) }

    /** US27 E2 y E3: el backend rechaza `END_OF_SHIFT` si el turno sigue vigente (422) y su `message` se muestra. */
    fun onPause(reason: PauseReason) {
        if (!_uiState.value.canPause) return
        // El tiempo efectivo se calcula antes de pausar: después el backend ya no lo permite deducir.
        val state = _uiState.value
        val elapsed = state.startedAt?.let { LaborTime.effectiveSeconds(it, state.totalPausedSeconds, clock.instant()) }
        _uiState.update { it.copy(pauseForm = null) }
        run(TaskAction.PAUSE, frozenAtPause = elapsed) { pauseTask(taskId, reason) }
    }

    /** US30: abre el cierre con las horas sugeridas (`startedAt` menos lo pausado). */
    fun onCompleteClick() {
        val state = _uiState.value
        if (!state.canClose) return
        val suggested = state.task?.let { suggestLaborHours(it) }
            ?: state.detail?.startedAt?.let {
                LaborTime.suggestedHours(it, state.detail.totalPausedSeconds, clock.instant())
            }
        val hours = suggested?.stripTrailingZeros()?.toPlainString()?.takeIf { it.isNotBlank() } ?: ""
        _uiState.update { it.copy(completeForm = CompleteForm(hours), notice = null, error = null, invalidField = null) }
    }

    fun onCompleteDismiss() = _uiState.update {
        if (it.isBusy) it else it.copy(completeForm = null, error = null, invalidField = null)
    }

    fun onHoursChange(value: String) = editComplete { it.copy(hours = value) }

    fun onNotesChange(value: String) = editComplete { it.copy(notes = value) }

    private fun editComplete(change: (CompleteForm) -> CompleteForm) = _uiState.update {
        val form = it.completeForm
        if (form == null || it.isBusy) it else it.copy(completeForm = change(form), error = null, invalidField = null)
    }

    fun onCompleteConfirm() {
        val state = _uiState.value
        val form = state.completeForm ?: return
        if (state.isBusy) return
        if (state.requiresAssemblyEvidence) {
            _uiState.update {
                it.copy(error = UiText.Resource(R.string.tasks_seg2_complete_missing_assembly_warning))
            }
            return
        }
        val hours = form.hours.toAmount()
        if (hours == null) {
            fail(InvalidTaskInputException("actualLaborHours"))
            return
        }
        _uiState.update { it.copy(busyAction = TaskAction.COMPLETE, error = null, invalidField = null) }
        viewModelScope.launch {
            completeTask(taskId, hours, form.notes.trim().ifBlank { null })
                .onSuccess { detail ->
                    pausedElapsed.clear(taskId)
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            completeForm = null,
                            detail = detail,
                            completed = true,
                            pausedElapsedSeconds = null
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    // --- US28: evidencias fotográficas de desmontaje y montaje (100 % offline-safe) ---

    fun onEvidenceKindChange(kind: EvidenceKind) = _uiState.update {
        it.copy(evidenceKind = kind, error = null, invalidField = null)
    }

    fun onEvidenceNoteChange(note: String) = _uiState.update {
        it.copy(evidenceNote = note, error = null, invalidField = null)
    }

    fun onEvidenceCaptured(photo: File) {
        val state = _uiState.value
        if (!state.canUploadPhotos || state.isBusy) return
        _uiState.update { it.copy(busyAction = TaskAction.ATTACH_EVIDENCE, notice = null, error = null, invalidField = null) }
        viewModelScope.launch {
            attachEvidence(taskId, state.resolvedWorkOrderId, photo, state.evidenceKind, state.evidenceNote)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            evidenceNote = "",
                            notice = UiText.Resource(R.string.tasks_seg2_evidence_queued)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    fun onCameraDenied() = _uiState.update {
        it.copy(error = UiText.Resource(R.string.gallery_camera_denied), notice = null)
    }

    fun onSelectFailedEvidence(pending: PendingEvidence) = _uiState.update {
        it.copy(selectedFailedEvidence = pending, error = null, notice = null)
    }

    fun onDismissFailedEvidence() = _uiState.update {
        it.copy(selectedFailedEvidence = null)
    }

    fun onRetryFailedEvidence() {
        val pending = _uiState.value.selectedFailedEvidence ?: return
        _uiState.update { it.copy(selectedFailedEvidence = null) }
        viewModelScope.launch {
            retryPendingOperation?.invoke(pending.operationId)
            _uiState.update { it.copy(notice = UiText.Resource(R.string.tasks_seg2_evidence_failed_retry_notice)) }
        }
    }

    fun onDiscardFailedEvidence() {
        val pending = _uiState.value.selectedFailedEvidence ?: return
        _uiState.update { it.copy(selectedFailedEvidence = null) }
        viewModelScope.launch {
            discardPendingOperation?.invoke(pending.operationId)
            _uiState.update { it.copy(notice = UiText.Resource(R.string.tasks_seg2_evidence_failed_discard_notice)) }
        }
    }

    // --- US29: suspensiones (mecánico solicita; supervisor aprueba, rechaza o levanta) ---

    private var suspensionSearchJob: Job? = null

    fun onRequestSuspensionClick() {
        if (!_uiState.value.canRequestSuspension) return
        _uiState.update {
            it.copy(suspensionRequestForm = RequestSuspensionForm(), notice = null, error = null, invalidField = null)
        }
    }

    fun onRequestSuspensionDismiss() {
        suspensionSearchJob?.cancel()
        _uiState.update {
            if (it.isBusy) it else it.copy(suspensionRequestForm = null, error = null, invalidField = null)
        }
    }

    private fun editSuspensionRequest(change: (RequestSuspensionForm) -> RequestSuspensionForm) = _uiState.update {
        val form = it.suspensionRequestForm
        if (form == null || it.isBusy) it else it.copy(suspensionRequestForm = change(form), error = null, invalidField = null)
    }

    fun onSuspensionMissingItemChange(value: String) = editSuspensionRequest {
        it.copy(missingItemDescription = value)
    }

    fun onSuspensionPartQueryChange(value: String) {
        if (!_uiState.value.canReadPartsCatalog) return
        suspensionSearchJob?.cancel()
        if (value.isBlank()) {
            editSuspensionRequest { it.copy(query = value, results = emptyList(), isSearching = false) }
            return
        }
        editSuspensionRequest { it.copy(query = value) }
        suspensionSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            editSuspensionRequest { it.copy(isSearching = true) }
            searchParts(value.trim())
                .onSuccess { parts -> editSuspensionRequest { it.copy(results = parts, isSearching = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            suspensionRequestForm = it.suspensionRequestForm?.copy(isSearching = false),
                            error = e.toTaskUiText()
                        )
                    }
                }
        }
    }

    fun onSuspensionPartSelect(part: PartOption?) {
        suspensionSearchJob?.cancel()
        editSuspensionRequest {
            val description = if (it.missingItemDescription.isBlank() && part != null) {
                part.name
            } else {
                it.missingItemDescription
            }
            it.copy(selectedPart = part, missingItemDescription = description, isSearching = false)
        }
    }

    fun onRequestSuspensionConfirm() {
        val state = _uiState.value
        val form = state.suspensionRequestForm ?: return
        if (!state.canRequestSuspension || state.isBusy) return
        _uiState.update { it.copy(busyAction = TaskAction.REQUEST_HOLD, error = null, invalidField = null) }
        viewModelScope.launch {
            requestSuspension(taskId, form.missingItemDescription, form.selectedPart?.id)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            suspensionRequestForm = null,
                            detail = detail,
                            notice = UiText.Resource(R.string.tasks_seg2_suspension_requested_notice)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    fun onReviewSuspensionClick() {
        if (!_uiState.value.canReviewSuspension) return
        _uiState.update {
            it.copy(reviewSuspensionForm = ReviewSuspensionForm(), notice = null, error = null, invalidField = null)
        }
    }

    fun onReviewSuspensionDismiss() = _uiState.update {
        if (it.isBusy) it else it.copy(reviewSuspensionForm = null, error = null)
    }

    fun onApproveSuspension() {
        val state = _uiState.value
        if (!state.canValidateHold || state.isBusy) return
        _uiState.update { it.copy(busyAction = TaskAction.APPROVE_HOLD, error = null, notice = null) }
        viewModelScope.launch {
            approveSuspension(taskId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            reviewSuspensionForm = null,
                            detail = detail,
                            notice = UiText.Resource(R.string.orders_suspension_approved_notice)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    fun onRejectSuspension() {
        val state = _uiState.value
        if (!state.canValidateHold || state.isBusy) return
        _uiState.update { it.copy(busyAction = TaskAction.REJECT_HOLD, error = null, notice = null) }
        viewModelScope.launch {
            rejectSuspension(taskId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            reviewSuspensionForm = null,
                            detail = detail,
                            notice = UiText.Resource(R.string.orders_suspension_rejected_notice)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    fun onResumeSuspendedTask() {
        val state = _uiState.value
        if (!state.canValidateHold || state.isBusy) return
        _uiState.update { it.copy(busyAction = TaskAction.RESUME_HOLD, error = null, notice = null) }
        viewModelScope.launch {
            resumeSuspendedTask(taskId)
                .onSuccess { detail ->
                    pausedElapsed.clear(taskId)
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            reviewSuspensionForm = null,
                            detail = detail,
                            pausedElapsedSeconds = null,
                            notice = UiText.Resource(R.string.orders_task_resumed_notice)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    // --- Reabrir tarea cerrada (`operations:work_orders:update`) ---

    fun onReopenClick() {
        if (!_uiState.value.canReopen) return
        _uiState.update { it.copy(reopenForm = ReopenTaskForm(), notice = null, error = null) }
    }

    fun onReopenDismiss() = _uiState.update {
        if (it.isBusy) it else it.copy(reopenForm = null, error = null)
    }

    fun onReopenReasonChange(value: String) = _uiState.update {
        val form = it.reopenForm
        if (form == null || it.isBusy) it else it.copy(reopenForm = form.copy(reason = value), error = null)
    }

    fun onReopenConfirm() {
        val state = _uiState.value
        val form = state.reopenForm ?: return
        if (!state.canReopenTask || state.isBusy) return
        _uiState.update { it.copy(busyAction = TaskAction.REOPEN, error = null, notice = null) }
        viewModelScope.launch {
            reopenTask(taskId, form.reason)
                .onSuccess { detail ->
                    pausedElapsed.clear(taskId)
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            reopenForm = null,
                            detail = detail,
                            completed = false,
                            pausedElapsedSeconds = null,
                            notice = UiText.Resource(R.string.tasks_seg2_reopened_notice)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    // --- US32 E2/E3: imputar un repuesto a la tarea ---

    private var searchJob: Job? = null

    fun onChargeClick() {
        if (!_uiState.value.canOpenCharge) return
        _uiState.update { it.copy(chargeForm = ChargeForm(), notice = null, error = null, invalidField = null) }
    }

    fun onChargeDismiss() {
        searchJob?.cancel()
        _uiState.update { if (it.isBusy) it else it.copy(chargeForm = null, error = null, invalidField = null) }
    }

    private fun editCharge(change: (ChargeForm) -> ChargeForm) = _uiState.update {
        val form = it.chargeForm
        if (form == null || it.isBusy) it else it.copy(chargeForm = change(form), error = null, invalidField = null)
    }

    /** Busca en línea por nombre o SKU con debounce; un texto vacío limpia la lista. */
    fun onChargeQueryChange(value: String) {
        searchJob?.cancel()
        if (value.isBlank()) {
            editCharge { it.copy(query = value, selected = null, results = emptyList(), isSearching = false) }
            return
        }
        editCharge { it.copy(query = value, selected = null) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            editCharge { it.copy(isSearching = true) }
            searchParts(value.trim())
                .onSuccess { parts -> editCharge { it.copy(results = parts, isSearching = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(chargeForm = it.chargeForm?.copy(isSearching = false), error = e.toTaskUiText())
                    }
                }
        }
    }

    /** Un repuesto agotado no se puede elegir (US31 E3). */
    fun onPartSelect(part: PartOption) {
        if (!part.isAvailable) return
        editCharge { it.copy(selected = part) }
    }

    fun onChargeQuantityChange(value: String) = editCharge { it.copy(quantity = value) }

    fun onChargeConfirm() {
        val state = _uiState.value
        val form = state.chargeForm ?: return
        val part = form.selected ?: return
        if (state.isBusy) return
        val quantity = form.quantity.toAmount()
        if (quantity == null) {
            fail(InvalidTaskInputException("quantity"))
            return
        }
        _uiState.update { it.copy(busyAction = TaskAction.CHARGE, error = null, invalidField = null) }
        viewModelScope.launch {
            // El descargo FIFO lo hace el backend; 409 con el message si pide más de lo que hay.
            addPart(taskId, part.id, quantity)
                .onSuccess { reloadDetail(UiText.Resource(R.string.task_part_charged)) }
                .onFailure(::fail)
        }
    }

    // --- US33: corregir la cantidad o devolver el repuesto al lote de origen ---

    fun onEditPartClick(product: TaskProduct) = openPartEdit(product, PartEditMode.EDIT_QUANTITY)

    fun onReturnPartClick(product: TaskProduct) = openPartEdit(product, PartEditMode.RETURN)

    private fun openPartEdit(product: TaskProduct, mode: PartEditMode) {
        val state = _uiState.value
        if (!state.canEditParts || state.isBusy) return
        _uiState.update {
            it.copy(
                partEdit = PartEditForm(product, mode, product.quantity.stripTrailingZeros().toPlainString()),
                notice = null,
                error = null,
                invalidField = null
            )
        }
    }

    fun onPartEditDismiss() = _uiState.update {
        if (it.isBusy) it else it.copy(partEdit = null, error = null, invalidField = null)
    }

    fun onPartQuantityChange(value: String) = _uiState.update {
        val edit = it.partEdit
        if (edit == null || it.isBusy) it else it.copy(partEdit = edit.copy(quantity = value), error = null, invalidField = null)
    }

    fun onPartEditConfirm() {
        val state = _uiState.value
        val edit = state.partEdit ?: return
        if (!state.canEditParts || state.isBusy) return
        when (edit.mode) {
            PartEditMode.RETURN -> {
                _uiState.update { it.copy(busyAction = TaskAction.REMOVE_PART, error = null, invalidField = null) }
                viewModelScope.launch {
                    // Las unidades vuelven al lote del que salieron y la orden rebaja el costo imputado.
                    removePart(taskId, edit.product.productId)
                        .onSuccess { reloadDetail(UiText.Resource(R.string.task_part_returned)) }
                        .onFailure(::fail)
                }
            }
            PartEditMode.EDIT_QUANTITY -> {
                val quantity = edit.quantity.toAmount()
                if (quantity == null) {
                    fail(InvalidTaskInputException("quantity"))
                    return
                }
                _uiState.update { it.copy(busyAction = TaskAction.EDIT_PART, error = null, invalidField = null) }
                viewModelScope.launch {
                    updatePartQuantity(taskId, edit.product.productId, quantity)
                        .onSuccess { detail ->
                            _uiState.update {
                                it.copy(
                                    busyAction = null,
                                    partEdit = null,
                                    detail = detail,
                                    notice = UiText.Resource(R.string.task_part_updated)
                                )
                            }
                        }
                        .onFailure(::fail)
                }
            }
        }
    }

    /** Recarga el detalle tras imputar o devolver: esas respuestas no traen la tarea completa. */
    private suspend fun reloadDetail(notice: UiText) {
        getDetail(taskId)
            .onSuccess { detail ->
                _uiState.update {
                    it.copy(busyAction = null, chargeForm = null, partEdit = null, detail = detail, notice = notice)
                }
            }
            .onFailure { e ->
                // La operación se hizo: se cierra el formulario y se avisa que falta refrescar.
                _uiState.update {
                    it.copy(
                        busyAction = null,
                        chargeForm = null,
                        partEdit = null,
                        notice = notice,
                        error = e.toTaskUiText()
                    )
                }
            }
    }

    private fun run(
        action: TaskAction,
        frozenAtPause: Long? = null,
        call: suspend () -> Result<TaskDetail>
    ) {
        _uiState.update { it.copy(busyAction = action, notice = null, error = null, invalidField = null) }
        viewModelScope.launch {
            call()
                .onSuccess { detail ->
                    // Al pausar se guarda el tiempo efectivo; al reanudar se descarta.
                    val frozen = if (detail.status == TaskStatus.ON_HOLD) {
                        frozenAtPause?.also { pausedElapsed.save(taskId, it) }
                    } else {
                        pausedElapsed.clear(taskId)
                        null
                    }
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            detail = detail,
                            notice = noticeFor(action),
                            pausedElapsedSeconds = frozen
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    private fun noticeFor(action: TaskAction): UiText? = when (action) {
        TaskAction.START -> UiText.Resource(R.string.task_started)
        TaskAction.PAUSE -> UiText.Resource(R.string.task_paused)
        else -> null
    }

    /** Acepta coma o punto decimal; null si no es un número. */
    private fun String.toAmount(): BigDecimal? = trim().replace(',', '.').toBigDecimalOrNull()

    private fun fail(error: Throwable) = _uiState.update {
        it.copy(busyAction = null, error = error.toTaskUiText(), invalidField = error.invalidTaskField())
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
