package com.andeva.atelier.mobile.features.tasks.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * Una tarea abierta del mecánico (`AssignedTaskResource` + lo que se guardó del detalle). [plate] y [bayName]
 * salen de las cachés `vehicles` y `bays`; null si aún no se cargaron o no hay permiso para leerlas.
 */
data class Task(
    val id: TaskId,
    val workOrderId: String,
    val workOrderNumber: Int?,
    val workOrderStatus: String?,
    val vehicleId: String,
    val plate: String?,
    val branchId: String?,
    val bayId: String?,
    val bayName: String?,
    val serviceName: String?,                         // solo tras abrir el detalle
    val mechanicId: String?,
    val status: TaskStatus?,                          // null si el backend envía un estado nuevo
    val description: String,
    val estimatedHours: BigDecimal?,
    val holdReason: HoldReason?,
    val missingItemDescription: String?,
    val pendingSuspensionReason: String?,
    val startedAt: Instant?,
    val totalPausedSeconds: Long
) {
    /** En pausa por refrigerio o fin de turno: el mecánico la reanuda con `StartTaskUseCase`. */
    val isPausedByMechanic: Boolean get() = isPausedByMechanic(status, holdReason)

    /** Suspendida por un repuesto: solo la levanta un supervisor (`ResumeSuspendedTaskUseCase`). */
    val isSuspended: Boolean get() = isSuspended(status, holdReason)

    /** Solicitud de suspensión enviada, a la espera de que un supervisor la apruebe o rechace. */
    val hasPendingSuspension: Boolean get() = pendingSuspensionReason != null
}

internal fun isPausedByMechanic(status: TaskStatus?, holdReason: HoldReason?) =
    status == TaskStatus.ON_HOLD && holdReason?.isMechanicPause == true

internal fun isSuspended(status: TaskStatus?, holdReason: HoldReason?) =
    status == TaskStatus.ON_HOLD && holdReason != null && !holdReason.isMechanicPause
