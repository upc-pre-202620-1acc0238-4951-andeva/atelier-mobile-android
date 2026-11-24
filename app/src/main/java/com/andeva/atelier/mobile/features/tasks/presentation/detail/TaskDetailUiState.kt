package com.andeva.atelier.mobile.features.tasks.presentation.detail

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.tasks.domain.EvidenceKind
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PendingEvidence
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskEvidence
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import java.time.Instant

/** Acción en línea en curso: bloquea el resto de botones mientras responde el backend. */
enum class TaskAction {
    START,
    PAUSE,
    COMPLETE,
    CHARGE,
    EDIT_PART,
    REMOVE_PART,
    REQUEST_HOLD,
    APPROVE_HOLD,
    REJECT_HOLD,
    RESUME_HOLD,
    REOPEN,
    ATTACH_EVIDENCE
}

/** US27 E2: hoja para tipificar la pausa. */
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

/** US29 E1: solicitud de suspensión por repuesto faltante o defectuoso + repuesto opcional del catálogo. */
data class RequestSuspensionForm(
    val missingItemDescription: String = "",
    val query: String = "",
    val results: List<PartOption> = emptyList(),
    val isSearching: Boolean = false,
    val selectedPart: PartOption? = null
)

/** US29 E1/E2/E3: revisión del supervisor sobre una solicitud pendiente o suspensión activa. */
data class ReviewSuspensionForm(val isOpen: Boolean = true)

/** Reapertura de tarea cerrada para retrabajo de calidad (`operations:work_orders:update`). */
data class ReopenTaskForm(val reason: String = "")

data class TaskDetailUiState(
    /** Tarea desde Room: se muestra sin red. */
    val task: Task? = null,
    /** Detalle en línea: servicio, productos imputados y evidencias. */
    val detail: TaskDetail? = null,
    val fallbackWorkOrderId: String = "",
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val busyAction: TaskAction? = null,
    val pauseForm: PauseForm? = null,
    val completeForm: CompleteForm? = null,
    val chargeForm: ChargeForm? = null,
    val partEdit: PartEditForm? = null,
    val suspensionRequestForm: RequestSuspensionForm? = null,
    val reviewSuspensionForm: ReviewSuspensionForm? = null,
    val reopenForm: ReopenTaskForm? = null,
    val pendingEvidence: List<PendingEvidence> = emptyList(),
    val selectedFailedEvidence: PendingEvidence? = null,
    val evidenceKind: EvidenceKind = EvidenceKind.DISASSEMBLY,
    val evidenceNote: String = "",
    val notice: UiText? = null,
    val error: UiText? = null,
    /** Campo que no pasó la validación (`actualLaborHours`, `notes`, `quantity`, `missingItemDescription`, `description`). */
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
    val canEditParts: Boolean = false,
    val canReadPartsCatalog: Boolean = false,
    val canUploadPhotos: Boolean = false,
    val canRequestHold: Boolean = false,
    val canValidateHold: Boolean = false,
    val canReportFinding: Boolean = false,
    val canOpenObd: Boolean = false,
    val canReopenTask: Boolean = false
) {
    val isBusy: Boolean get() = busyAction != null

    val hasOpenSheet: Boolean
        get() = pauseForm != null ||
            completeForm != null ||
            chargeForm != null ||
            partEdit != null ||
            suspensionRequestForm != null ||
            reviewSuspensionForm != null ||
            reopenForm != null ||
            selectedFailedEvidence != null

    /** D3: los productos solo vienen de `GET /tasks/{id}`; sin ese detalle no se sabe qué repuestos hay. */
    val partsAvailable: Boolean get() = detail != null

    // El detalle en línea manda sobre la caché una vez que llega o tras una acción.
    val status: TaskStatus? get() = detail?.status ?: task?.status
    val holdReason: HoldReason? get() = if (detail != null) detail.holdReason else task?.holdReason
    val startedAt: Instant? get() = detail?.startedAt ?: task?.startedAt
    val totalPausedSeconds: Long get() = detail?.totalPausedSeconds ?: task?.totalPausedSeconds ?: 0L
    val description: String get() = detail?.description ?: task?.description.orEmpty()
    val products: List<TaskProduct> get() = detail?.products.orEmpty()
    val evidenceImages: List<TaskEvidence> get() = detail?.evidenceImages.orEmpty()

    val plate: String? get() = task?.plate
    val vehicleId: String? get() = task?.vehicleId
    val resolvedWorkOrderId: String get() = detail?.workOrderId ?: task?.workOrderId ?: fallbackWorkOrderId

    val missingItemDescription: String? get() = detail?.missingItemDescription ?: task?.missingItemDescription
    val pendingSuspensionReason: String?
        get() = if (detail != null) detail.pendingSuspensionReason else task?.pendingSuspensionReason
    val pendingSuspensionRequestedAt: Instant?
        get() = detail?.pendingSuspensionRequestedAt ?: task?.pendingSuspensionRequestedAt
    val hasPendingSuspension: Boolean
        get() = pendingSuspensionRequestedAt != null || !pendingSuspensionReason.isNullOrBlank()

    val maxEvidenceNoteLength: Int
        get() = 500 - (evidenceKind.label.length + 2)

    val hasAssemblyEvidence: Boolean
        get() = evidenceImages.any { it.description.isAssemblyDescription() } ||
            pendingEvidence.any { it.description.isAssemblyDescription() }

    /** Exige foto de montaje antes de cerrar si el perfil puede subir evidencia fotográfica. */
    val requiresAssemblyEvidence: Boolean
        get() = canUploadPhotos && !hasAssemblyEvidence

    val hasUnsavedChanges: Boolean
        get() = evidenceNote.isNotBlank()

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

    /** US29 E1 (mecánico): pedir suspensión mientras la tarea está abierta y sin solicitud ni suspensión activa. */
    val canRequestSuspension: Boolean
        get() = canRequestHold && !isBusy && !hasPendingSuspension && !isSuspended &&
            (status == TaskStatus.IN_PROGRESS || status == TaskStatus.ASSIGNED || isPausedByMechanic)

    /** US29 (supervisor): aprobar/rechazar solicitud pendiente o levantar suspensión activa (H5/H6). */
    val canReviewSuspension: Boolean
        get() = canValidateHold && !isBusy && (hasPendingSuspension || isSuspended)

    /** Reabrir tarea cerrada (`operations:work_orders:update`). */
    val canReopen: Boolean
        get() = canReopenTask && !isBusy && status == TaskStatus.COMPLETED

    val hasAnyActionPermission: Boolean
        get() = canTrackTime || canComplete || canChargeParts || canEditParts ||
            canUploadPhotos || canRequestHold || canValidateHold || canReopenTask
}

private fun String?.isAssemblyDescription(): Boolean {
    val text = this?.trim().orEmpty()
    if (text.isEmpty()) return false
    return text.startsWith(EvidenceKind.ASSEMBLY.label, ignoreCase = true) ||
        (text.contains(EvidenceKind.ASSEMBLY.label, ignoreCase = true) &&
            !text.contains(EvidenceKind.DISASSEMBLY.label, ignoreCase = true))
}
