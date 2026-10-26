package com.andeva.atelier.mobile.features.workorders.infrastructure.remote

import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * `WorkOrderSummaryResource` (lista), `WorkOrderResource` (acciones) y `WorkOrderDetailResource` (detalle) de
 * docs/api/operations.md en un solo DTO: los campos que una respuesta no trae llegan en null.
 */
data class WorkOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("internalNumber") val internalNumber: Int?,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("customerId") val customerId: String,
    @SerializedName("branchId") val branchId: String,
    @SerializedName("currentBayId") val currentBayId: String?,
    @SerializedName("bayName") val bayName: String?,
    @SerializedName("mileageIn") val mileageIn: Int?,
    @SerializedName("diagnosticSummary") val diagnosticSummary: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("subtotal") val subtotal: BigDecimal?,
    @SerializedName("taxAmount") val taxAmount: BigDecimal?,
    @SerializedName("totalAmount") val totalAmount: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("tasks") val tasks: List<WorkOrderTaskDto>?,
    @SerializedName("proposals") val proposals: List<TaskProposalDto>?,
    @SerializedName("intakeImages") val intakeImages: List<WorkOrderImageDto>?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

/** `TaskProposalResource`. */
data class TaskProposalDto(
    @SerializedName("id") val id: String,
    @SerializedName("workOrderId") val workOrderId: String,
    @SerializedName("taskId") val taskId: String?,
    @SerializedName("serviceId") val serviceId: String?,
    @SerializedName("mechanicId") val mechanicId: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("customerNotes") val customerNotes: String?,
    @SerializedName("createdAt") val createdAt: String?
)

/** `WorkOrderImageResource`. */
data class WorkOrderImageDto(
    @SerializedName("id") val id: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("description") val description: String?,
    @SerializedName("uploadedAt") val uploadedAt: String?
)

/** `ServiceResource`. */
data class ServiceDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("basePrice") val basePrice: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("estimatedMinutes") val estimatedMinutes: Int?
)

/** `PartMovementResource`. */
data class PartMovementDto(
    @SerializedName("allocationId") val allocationId: String,
    @SerializedName("productId") val productId: String,
    @SerializedName("taskId") val taskId: String?,
    @SerializedName("batchId") val batchId: String?,
    @SerializedName("chargedQuantity") val chargedQuantity: BigDecimal?,
    @SerializedName("returnedQuantity") val returnedQuantity: BigDecimal?,
    @SerializedName("unitCost") val unitCost: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("chargedAt") val chargedAt: String?,
    @SerializedName("lastReturnedAt") val lastReturnedAt: String?,
    @SerializedName("lastReturnedBy") val lastReturnedBy: String?,
    @SerializedName("lastReturnReason") val lastReturnReason: String?
)

/** `WorkOrderProfitabilityResource`: un objeto (no un array, como decía operations.md). */
data class WorkOrderProfitabilityDto(
    @SerializedName("workOrderId") val workOrderId: String,
    @SerializedName("laborRevenue") val laborRevenue: BigDecimal?,
    @SerializedName("partsRevenue") val partsRevenue: BigDecimal?,
    @SerializedName("partsCostOfSales") val partsCostOfSales: BigDecimal?,
    @SerializedName("partsGrossProfit") val partsGrossProfit: BigDecimal?,
    @SerializedName("subtotal") val subtotal: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("lines") val lines: List<PartProfitabilityDto>?
)

/** `PartProfitabilityResource`. */
data class PartProfitabilityDto(
    @SerializedName("taskId") val taskId: String?,
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: BigDecimal?,
    @SerializedName("unitPrice") val unitPrice: BigDecimal?,
    @SerializedName("revenue") val revenue: BigDecimal?,
    @SerializedName("costOfSales") val costOfSales: BigDecimal?,
    @SerializedName("grossProfit") val grossProfit: BigDecimal?
)

/** Respuesta de `notify` y `quotation/send`: [recipient] es el correo del cliente. */
data class EmailSentDto(
    @SerializedName("message") val message: String?,
    @SerializedName("recipient") val recipient: String?
)

/** `CreateWorkOrderResource`. */
data class CreateWorkOrderRequestDto(
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("customerId") val customerId: String,
    @SerializedName("branchId") val branchId: String,
    @SerializedName("mileageIn") val mileageIn: Int,
    @SerializedName("diagnosticSummary") val diagnosticSummary: String?,
    @SerializedName("appointmentId") val appointmentId: String?
)

/** `UpdateWorkOrderResource`. */
data class UpdateWorkOrderRequestDto(
    @SerializedName("mileageIn") val mileageIn: Int?,
    @SerializedName("diagnosticSummary") val diagnosticSummary: String?
)

/** `CreateWorkOrderTaskResource`; `mechanicId` es el membershipId. */
data class CreateWorkOrderTaskRequestDto(
    @SerializedName("serviceId") val serviceId: String,
    @SerializedName("description") val description: String,
    @SerializedName("mechanicId") val mechanicId: String?,
    @SerializedName("price") val price: BigDecimal?
)

/** `UpdateWorkOrderTaskResource`. */
data class UpdateWorkOrderTaskRequestDto(
    @SerializedName("description") val description: String?,
    @SerializedName("mechanicId") val mechanicId: String?,
    @SerializedName("price") val price: BigDecimal?
)

/** `AttachImageResource` (`ATTACH_INTAKE_IMAGE`). */
data class AttachImageRequestDto(
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("description") val description: String?
)

/** `SubmitTaskProposalResource` (`SUBMIT_PROPOSAL`): `severity` solo `LOW|MEDIUM|CRITICAL`. */
data class SubmitProposalRequestDto(
    @SerializedName("mechanicId") val mechanicId: String,
    @SerializedName("description") val description: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("suggestedServiceId") val suggestedServiceId: String?
)

/** `ApproveTaskProposalResource`. */
data class ApproveProposalRequestDto(
    @SerializedName("serviceId") val serviceId: String,
    @SerializedName("finalPrice") val finalPrice: BigDecimal,
    @SerializedName("laborHours") val laborHours: BigDecimal,
    @SerializedName("mechanicId") val mechanicId: String?,
    @SerializedName("notes") val notes: String?
)

/** `RejectTaskProposalResource`. */
data class RejectProposalRequestDto(
    @SerializedName("customerNotes") val customerNotes: String
)

/** `CancelWorkOrderResource`: motivo de rechazo de la proforma y de cancelación de la orden. */
data class ReasonRequestDto(
    @SerializedName("reason") val reason: String
)

/** Payload de `ATTACH_INTAKE_IMAGE` en la cola: el id va en la ruta. */
data class AttachIntakeImagePayload(
    val workOrderId: String,
    val imageUrl: String,
    val description: String?
)

/** Payload de `SUBMIT_PROPOSAL` en la cola: el id va en la ruta. */
data class SubmitProposalPayload(
    val workOrderId: String,
    val mechanicId: String,
    val description: String,
    val severity: String,
    val imageUrl: String,
    val suggestedServiceId: String?
)
