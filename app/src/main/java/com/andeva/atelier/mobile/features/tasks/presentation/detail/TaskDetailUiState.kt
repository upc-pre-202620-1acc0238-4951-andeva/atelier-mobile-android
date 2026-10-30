package com.andeva.atelier.mobile.features.tasks.presentation.detail

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import java.time.Instant

/** Acción en línea en curso: bloquea el resto de botones mientras responde el backend. */
enum class TaskAction { START, PAUSE, COMPLETE, CHARGE, EDIT_PART, REMOVE_PART }

/** US27 E2: modal para tipificar la pausa. */
data class PauseForm(val isOpen: Boolean = true)

/** US30: cierre con las horas efectivas sugeridas, que el mecánico confirma o corrige. */
data class CompleteForm(val hours: String, val notes: String = "")

/** US32 E2: buscar el repuesto, elegirlo y fijar la cantidad a imputar a la tarea. */
data class ChargeForm(
    val query: String = "",
    val results: List<PartOption> = emptyList(),
    val isSearching: Boolean = false,
    val selected: PartOption? = null,
    val quantity: String = "1"
)

enum class PartEditMode { EDIT_QUANTITY, RETURN }

/** US33: cambiar la cantidad de un repuesto imputado o devolverlo a su lote de origen. */
data class PartEditForm(val product: TaskProduct, val mode: PartEditMode, val quantity: String)

data class TaskDetailUiState(
    /** Tarea desde Room: se muestra sin red. */
    val task: Task? = null,
    /** Detalle en línea: servicio, productos imputados y evidencias. */
    val detail: TaskDetail? = null,
    val isLoading: Boolean = true,
    val busyAction: TaskAction? = null,
    val pauseForm: PauseForm? = null,
    val completeForm: CompleteForm? = null,
    val chargeForm: ChargeForm? = null,
    val partEdit: PartEditForm? = null,
    val notice: UiText? = null,
    val error: UiText? = null,
    /** Campo que no pasó la validación (`actualLaborHours`, `notes`, `quantity`). */
    val invalidField: String? = null,
    /** La tarea se cerró: la pantalla vuelve a la bandeja. */
    val completed: Boolean = false,
    /**
     * D1: tiempo efectivo al pausar, guardado en este teléfono. null si la pausa no la hizo esta app (el backend no
     * envía la hora de la pausa), y el cronómetro no inventa un valor.
     */
    val pausedElapsedSeconds: Long? = null,
    val canTrackTime: Boolean = false,
    val canComplete: Boolean = false,
    val canChargeParts: Boolean = false,
    val canEditParts: Boolean = false
) {
    val isBusy: Boolean get() = busyAction != null

    /** D3: los productos solo vienen de `GET /tasks/{id}`; sin ese detalle no se sabe qué repuestos hay. */
    val partsAvailable: Boolean get() = detail != null

    // El detalle en línea manda sobre la caché una vez que llega o tras una acción.
    val status: TaskStatus? get() = detail?.status ?: task?.status
    val holdReason: HoldReason? get() = if (detail != null) detail.holdReason else task?.holdReason
    val startedAt: Instant? get() = detail?.startedAt ?: task?.startedAt
    val totalPausedSeconds: Long get() = detail?.totalPausedSeconds ?: task?.totalPausedSeconds ?: 0L
    val description: String get() = detail?.description ?: task?.description.orEmpty()
    val products: List<TaskProduct> get() = detail?.products.orEmpty()

    val isPausedByMechanic: Boolean
        get() = status == TaskStatus.ON_HOLD && holdReason?.isMechanicPause == true
    val isSuspended: Boolean
        get() = status == TaskStatus.ON_HOLD && holdReason != null && holdReason?.isMechanicPause == false

    /** US27 E1 y E5: iniciar la faena asignada o reanudar una pausa propia. */
    val canStart: Boolean
        get() = canTrackTime && !isBusy && (status == TaskStatus.ASSIGNED || isPausedByMechanic)
    val canPause: Boolean get() = canTrackTime && !isBusy && status == TaskStatus.IN_PROGRESS
    val canClose: Boolean get() = canComplete && !isBusy && status == TaskStatus.IN_PROGRESS

    /** Imputar repuestos solo con la tarea abierta. */
    val canOpenCharge: Boolean
        get() = canChargeParts && partsAvailable && !isBusy &&
            (status == TaskStatus.IN_PROGRESS || status == TaskStatus.ASSIGNED || status == TaskStatus.ON_HOLD)
}
