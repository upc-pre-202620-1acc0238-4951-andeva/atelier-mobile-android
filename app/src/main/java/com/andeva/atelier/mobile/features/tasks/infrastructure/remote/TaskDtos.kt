package com.andeva.atelier.mobile.features.tasks.infrastructure.remote

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** `AssignedTaskResource` de docs/api/operations.md (`GET /tasks/my`). */
data class AssignedTaskDto(
    @SerializedName("taskId") val taskId: String,
    @SerializedName("workOrderId") val workOrderId: String,
    @SerializedName("workOrderNumber") val workOrderNumber: Int?,
    @SerializedName("workOrderStatus") val workOrderStatus: String?,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("bayId") val bayId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("estimatedHours") val estimatedHours: BigDecimal?,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("totalPausedSeconds") val totalPausedSeconds: Long?
)

/** `WorkOrderTaskResource`: respuesta del detalle y de todas las acciones sobre la tarea. */
data class WorkOrderTaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("workOrderId") val workOrderId: String,
    @SerializedName("serviceId") val serviceId: String?,
    @SerializedName("serviceName") val serviceName: String?,
    @SerializedName("mechanicId") val mechanicId: String?,
    @SerializedName("mechanicName") val mechanicName: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("holdReason") val holdReason: String?,
    @SerializedName("missingItemDescription") val missingItemDescription: String?,
    @SerializedName("totalPausedSeconds") val totalPausedSeconds: Long?,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("completedAt") val completedAt: String?,
    @SerializedName("pendingSuspensionReason") val pendingSuspensionReason: String?,
    @SerializedName("pendingSuspensionRequestedAt") val pendingSuspensionRequestedAt: String?,
    @SerializedName("products") val products: List<TaskProductDto>?,
    @SerializedName("evidenceImages") val evidenceImages: List<WorkOrderTaskImageDto>?
)

/** `TaskProductResource`. */
data class TaskProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("taskId") val taskId: String?,
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String?,
    @SerializedName("quantity") val quantity: BigDecimal?,
    @SerializedName("unitPrice") val unitPrice: BigDecimal?,
    @SerializedName("totalAmount") val totalAmount: BigDecimal?,
    @SerializedName("currency") val currency: String?
)

/** `WorkOrderTaskImageResource`. */
data class WorkOrderTaskImageDto(
    @SerializedName("id") val id: String,
    @SerializedName("taskId") val taskId: String?,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("description") val description: String?,
    @SerializedName("uploadedAt") val uploadedAt: String?
)

/** `PauseTaskResource`: `reason` = `BREAK` o `END_OF_SHIFT`. */
data class PauseTaskRequestDto(
    @SerializedName("reason") val reason: String
)

/** `HoldTaskResource`. */
data class HoldTaskRequestDto(
    @SerializedName("missingItemDescription") val missingItemDescription: String,
    @SerializedName("inventoryItemId") val inventoryItemId: String? = null
)

/** `CompleteTaskResource`. */
data class CompleteTaskRequestDto(
    @SerializedName("actualLaborHours") val actualLaborHours: BigDecimal,
    @SerializedName("notes") val notes: String? = null
)

/** `AttachTaskEvidenceResource`. */
data class AttachTaskEvidenceRequestDto(
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("description") val description: String? = null
)

/** Payload de `ATTACH_TASK_EVIDENCE`: el body más el id de la ruta. */
data class AttachTaskEvidencePayload(
    @SerializedName("taskId") val taskId: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("description") val description: String?
)

/**
 * `AddTaskProductResource`. Sin `unitPrice` el backend copia el `basePrice` del catálogo (US35 E1);
 * Gson no serializa los null.
 */
data class AddTaskProductRequestDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: BigDecimal,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("unitPrice") val unitPrice: BigDecimal? = null
)

/** `UpdateTaskProductResource`. */
data class UpdateTaskProductRequestDto(
    @SerializedName("quantity") val quantity: BigDecimal
)
