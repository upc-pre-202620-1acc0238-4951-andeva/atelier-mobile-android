package com.andeva.atelier.mobile.features.tasks.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.time.Instant

/** Detalle completo de una tarea (`WorkOrderTaskResource`). Se lee en línea: productos y evidencias no se guardan en Room. */
data class TaskDetail(
    val id: TaskId,
    val workOrderId: String,
    val serviceId: String?,
    val serviceName: String?,
    val mechanicId: String?,
    val mechanicName: String?,
    val status: TaskStatus?,
    val description: String,
    val price: Money?,                                // sin IGV (docs/api/README.md §6)
    val holdReason: HoldReason?,
    val missingItemDescription: String?,
    val totalPausedSeconds: Long,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val pendingSuspensionReason: String?,
    val pendingSuspensionRequestedAt: Instant?,
    val products: List<TaskProduct>,
    val evidenceImages: List<TaskEvidence>
) {
    val isPausedByMechanic: Boolean get() = isPausedByMechanic(status, holdReason)
    val isSuspended: Boolean get() = isSuspended(status, holdReason)
    val hasPendingSuspension: Boolean get() = pendingSuspensionReason != null
}

/** Repuesto imputado a la tarea (`TaskProductResource`). El precio queda fijo al imputar (US35 E3). */
data class TaskProduct(
    val id: String,
    val productId: String,
    val productName: String?,
    val quantity: BigDecimal,
    val unitPrice: Money?,
    val totalAmount: Money?
)

/** Foto de desmontaje o montaje ya registrada (`WorkOrderTaskImageResource`). */
data class TaskEvidence(
    val id: String,
    val imageUrl: String,
    val description: String?,
    val uploadedAt: Instant?
)
