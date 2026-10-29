package com.andeva.atelier.mobile.features.tasks.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.CompleteTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.GetTaskDetailUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.PauseTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.StartTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SuggestLaborHoursUseCase
import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.presentation.invalidTaskField
import com.andeva.atelier.mobile.features.tasks.presentation.toTaskUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * US27: detalle de la tarea con cronómetro. Iniciar y reanudar (`/start`), pausar con motivo (`/pause`) y cerrar
 * con las horas efectivas (US30). Todo en línea; la tarea se ve sin red desde Room.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeTask: ObserveTaskUseCase,
    private val getDetail: GetTaskDetailUseCase,
    private val startTask: StartTaskUseCase,
    private val pauseTask: PauseTaskUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val suggestLaborHours: SuggestLaborHoursUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId = TaskId(savedStateHandle["taskId"] ?: "")

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canTrackTime = session?.has(Permission.TASKS_TRACK_TIME) == true,
                        canComplete = session?.has(Permission.TASKS_COMPLETE) == true,
                        canChargeParts = session?.has(Permission.TASKS_CHARGE_PARTS) == true ||
                            session?.has(Permission.WORK_ORDERS_UPDATE) == true,
                        canEditParts = session?.has(Permission.WORK_ORDERS_UPDATE) == true
                    )
                }
            }
        }
        viewModelScope.launch {
            observeTask(taskId).collect { task -> _uiState.update { it.copy(task = task) } }
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
        }
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

    /** US27 E2 y E3: el backend rechaza `END_OF_SHIFT` si el turno sigue vigente y su `message` se muestra. */
    fun onPause(reason: PauseReason) {
        if (!_uiState.value.canPause) return
        _uiState.update { it.copy(pauseForm = null) }
        run(TaskAction.PAUSE) { pauseTask(taskId, reason) }
    }

    /** US30: abre el cierre con las horas sugeridas (`startedAt` menos lo pausado). */
    fun onCompleteClick() {
        val state = _uiState.value
        if (!state.canClose) return
        val suggested = state.task?.let { suggestLaborHours(it) }
        val hours = suggested?.stripTrailingZeros()?.toPlainString()?.takeIf { it.isNotBlank() } ?: ""
        _uiState.update { it.copy(completeForm = CompleteForm(hours), notice = null, error = null, invalidField = null) }
    }

    fun onCompleteDismiss() = _uiState.update { if (it.isBusy) it else it.copy(completeForm = null, error = null, invalidField = null) }

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
        val hours = form.hours.toAmount()
        if (hours == null) {
            fail(InvalidTaskInputException("actualLaborHours"))
            return
        }
        _uiState.update { it.copy(busyAction = TaskAction.COMPLETE, error = null, invalidField = null) }
        viewModelScope.launch {
            completeTask(taskId, hours, form.notes.trim().ifBlank { null })
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(busyAction = null, completeForm = null, detail = detail, completed = true)
                    }
                }
                .onFailure(::fail)
        }
    }

    internal fun run(action: TaskAction, call: suspend () -> Result<TaskDetail>) {
        _uiState.update { it.copy(busyAction = action, notice = null, error = null, invalidField = null) }
        viewModelScope.launch {
            call()
                .onSuccess { detail ->
                    _uiState.update { it.copy(busyAction = null, detail = detail, notice = noticeFor(action)) }
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
    internal fun String.toAmount(): BigDecimal? = trim().replace(',', '.').toBigDecimalOrNull()

    internal fun fail(error: Throwable) = _uiState.update {
        it.copy(busyAction = null, error = error.toTaskUiText(), invalidField = error.invalidTaskField())
    }
}
