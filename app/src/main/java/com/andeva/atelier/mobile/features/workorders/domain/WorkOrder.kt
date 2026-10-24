package com.andeva.atelier.mobile.features.workorders.domain

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import java.time.Instant

/** `WorkOrderStatus` de docs/api/README.md §8. */
enum class WorkOrderStatus {
    DRAFT, IN_PROGRESS, COMPLETED, PAID, CANCELED;

    /** `PAID` y `CANCELED` ya no se trabajan: no se guardan en la caché de la sede (docs/06-offline-sync.md §4). */
    val isClosed: Boolean get() = this == PAID || this == CANCELED
}

/**
 * Cabecera de la orden (caché `work_orders`). Montos sin IGV en tareas y repuestos; el backend calcula [subtotal],
 * [taxAmount] (IGV 18 %) y [totalAmount] (US36). La lista de la sede no trae [subtotal], [taxAmount], [mileageIn] ni
 * [diagnosticSummary]: llegan con el detalle. [plate] y [customerName] salen de las cachés de recepción.
 */
data class WorkOrder(
    val id: String,
    val internalNumber: Int?,
    val branchId: String,
    val vehicleId: String,
    val customerId: String,
    val currentBayId: String?,
    val bayName: String?,
    val status: WorkOrderStatus?,
    val diagnosticSummary: String?,
    val mileageIn: Int?,
    val subtotal: Money?,
    val taxAmount: Money?,
    val totalAmount: Money?,
    val plate: String? = null,
    val customerName: String? = null
)

/**
 * Detalle en línea (`GET /work-orders/{id}`): tareas con repuestos y evidencias, propuestas y fotos de recepción.
 * Las tareas son [TaskDetail] de `tasks`: sus acciones (suspensión, repuestos) se hacen con esos casos de uso (H5).
 * `serviceName`, `mechanicName` y `productName` no traen el valor real (H11 de docs/03-traceability.md).
 */
data class WorkOrderDetail(
    val workOrder: WorkOrder,
    val tasks: List<TaskDetail>,
    val proposals: List<Proposal>,
    val intakeImages: List<IntakeImage>,
    val createdAt: Instant?,
    val updatedAt: Instant?
) {
    /** Tareas con una solicitud de suspensión esperando al supervisor (H5). */
    val tasksAwaitingSuspensionReview: List<TaskDetail> get() = tasks.filter { it.pendingSuspensionRequestedAt != null }
}

/** Foto de recepción ya registrada (`WorkOrderImageResource`, US13 E2). */
data class IntakeImage(
    val id: String,
    val imageUrl: String,
    val description: String?,
    val uploadedAt: Instant?
)

/** Apertura de orden (US12 E1): `CreateWorkOrderResource`; la sede es la de la sesión. */
data class NewWorkOrder(
    val vehicleId: String,
    val customerId: String,
    val mileageIn: Int,
    val diagnosticSummary: String? = null,
    val appointmentId: String? = null
)
