package com.andeva.atelier.mobile.features.inventory.infrastructure.remote

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** `PartResource` de docs/api/inventory.md. `tasks` también lo usa para buscar el repuesto a imputar. */
data class PartDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("sku") val sku: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("basePrice") val basePrice: BigDecimal?,
    @SerializedName("totalStock") val totalStock: BigDecimal?,
    @SerializedName("minimumStock") val minimumStock: BigDecimal?,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String?,
    @SerializedName("status") val status: String?,
    /** No existe en `PartResource` todavía (H35); queda null y Gson lo toma si el backend lo añade. */
    @SerializedName("brand") val brand: String? = null
)

/** `PartDetailResource`: el repuesto con todos sus lotes (también agotados, sin orden definido). */
data class PartDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("sku") val sku: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("basePrice") val basePrice: BigDecimal?,
    @SerializedName("totalStock") val totalStock: BigDecimal?,
    @SerializedName("minimumStock") val minimumStock: BigDecimal?,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("batches") val batches: List<BatchDto>?
) {
    fun toPart() = PartDto(id, name, sku, category, basePrice, totalStock, minimumStock, unitOfMeasure, status)
}

/** `BatchResource`. `arrivalDate` en UTC; los montos sin moneda están en PEN. */
data class BatchDto(
    @SerializedName("id") val id: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("supplierId") val supplierId: String?,
    @SerializedName("purchaseOrderId") val purchaseOrderId: String?,
    @SerializedName("batchNumber") val batchNumber: String?,
    @SerializedName("initialQty") val initialQty: BigDecimal?,
    @SerializedName("remainingQty") val remainingQty: BigDecimal?,
    @SerializedName("unitCost") val unitCost: BigDecimal?,
    @SerializedName("arrivalDate") val arrivalDate: String?,
    @SerializedName("receiptImageUrl") val receiptImageUrl: String?
)

/** `StockAlertResource` (calculada al consultar, sin id: H13). */
data class StockAlertDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("sku") val sku: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("currentStock") val currentStock: BigDecimal?,
    @SerializedName("minimumStock") val minimumStock: BigDecimal?,
    @SerializedName("deficit") val deficit: BigDecimal?
)

/** `{ valuation }` de `GET /inventory/items/valuation`, en PEN. */
data class ValuationDto(
    @SerializedName("valuation") val valuation: BigDecimal?
)

/** `PurchaseOrderResource`. */
data class PurchaseOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("supplierId") val supplierId: String?,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("orderNumber") val orderNumber: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("totalCost") val totalCost: BigDecimal?,
    @SerializedName("receiptImageUrl") val receiptImageUrl: String?,
    @SerializedName("receiptNumber") val receiptNumber: String?,
    @SerializedName("receivedAt") val receivedAt: String?,
    @SerializedName("items") val items: List<PurchaseOrderItemDto>?
)

/** `PurchaseOrderItemResource`. */
data class PurchaseOrderItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: BigDecimal?,
    @SerializedName("unitCost") val unitCost: BigDecimal?,
    @SerializedName("totalCost") val totalCost: BigDecimal?
)

/** `ReceivePurchaseOrderResource`: se envía siempre (sin cuerpo el backend inventa el comprobante). */
data class ReceivePurchaseOrderRequestDto(
    @SerializedName("receiptImageUrl") val receiptImageUrl: String,
    @SerializedName("receiptNumber") val receiptNumber: String,
    @SerializedName("receivedAt") val receivedAt: String?
)

/** `CreatePartResource` de docs/api/inventory.md. */
data class CreatePartRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("sku") val sku: String,
    @SerializedName("category") val category: String,
    @SerializedName("basePrice") val basePrice: BigDecimal,
    @SerializedName("minimumStock") val minimumStock: BigDecimal,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String? = null
)

/** `UpdatePartResource` de docs/api/inventory.md. */
data class UpdatePartRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("basePrice") val basePrice: BigDecimal,
    @SerializedName("minimumStock") val minimumStock: BigDecimal,
    @SerializedName("status") val status: String? = null,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String? = null
)

/** `SupplierResource` de docs/api/inventory.md. */
data class SupplierDto(
    @SerializedName("id") val id: String,
    @SerializedName("tenantId") val tenantId: String?,
    @SerializedName("businessName") val businessName: String?,
    @SerializedName("taxId") val taxId: String?,
    @SerializedName("contactName") val contactName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("active") val active: Boolean?
)

/** `RegisterSupplierResource` de docs/api/inventory.md. */
data class RegisterSupplierRequestDto(
    @SerializedName("businessName") val businessName: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("contactName") val contactName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null
)

/** `UpdateSupplierResource` de docs/api/inventory.md. */
data class UpdateSupplierRequestDto(
    @SerializedName("businessName") val businessName: String? = null,
    @SerializedName("contactName") val contactName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("active") val active: Boolean? = null
)

/** `CreatePurchaseOrderItemResource` de docs/api/inventory.md. */
data class CreatePurchaseOrderItemRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: BigDecimal,
    @SerializedName("unitCost") val unitCost: BigDecimal
)

/** `CreatePurchaseOrderResource` de docs/api/inventory.md. */
data class CreatePurchaseOrderRequestDto(
    @SerializedName("branchId") val branchId: String,
    @SerializedName("supplierId") val supplierId: String,
    @SerializedName("items") val items: List<CreatePurchaseOrderItemRequestDto>,
    @SerializedName("orderNumber") val orderNumber: String? = null,
    @SerializedName("expectedDeliveryDate") val expectedDeliveryDate: String? = null,
    @SerializedName("notes") val notes: String? = null
)

/** `CancelPurchaseOrderResource` de docs/api/inventory.md. */
data class CancelPurchaseOrderRequestDto(
    @SerializedName("reason") val reason: String? = null
)

/** `ReceiveBatchResource` de docs/api/inventory.md. */
data class ReceiveBatchRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("batchNumber") val batchNumber: String,
    @SerializedName("quantity") val quantity: BigDecimal,
    @SerializedName("unitCost") val unitCost: BigDecimal,
    @SerializedName("arrivalDate") val arrivalDate: String? = null,
    @SerializedName("purchaseOrderId") val purchaseOrderId: String? = null,
    @SerializedName("receiptImageUrl") val receiptImageUrl: String? = null,
    @SerializedName("supplierId") val supplierId: String? = null
)

/** `DispatchBatchFifoResource` de docs/api/inventory.md. */
data class DispatchBatchFifoRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: BigDecimal,
    @SerializedName("taskId") val taskId: String? = null,
    @SerializedName("workOrderId") val workOrderId: String? = null
)

/** `BatchDeductionResource` de docs/api/inventory.md. */
data class BatchDeductionDto(
    @SerializedName("batchId") val batchId: String,
    @SerializedName("quantityDeducted") val quantityDeducted: BigDecimal?,
    @SerializedName("unitCost") val unitCost: BigDecimal?,
    @SerializedName("subtotal") val subtotal: BigDecimal?
)

/** `StockAllocationResource` de docs/api/inventory.md. */
data class StockAllocationDto(
    @SerializedName("allocationId") val allocationId: String,
    @SerializedName("allocatedQuantity") val allocatedQuantity: BigDecimal?,
    @SerializedName("totalCogs") val totalCogs: BigDecimal?,
    @SerializedName("deductions") val deductions: List<BatchDeductionDto>?
)

/** `RestoreBatchResource` de docs/api/inventory.md. */
data class RestoreBatchRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: BigDecimal,
    @SerializedName("batchId") val batchId: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("workOrderId") val workOrderId: String? = null
)

/** Respuesta 200 de `POST /api/v1/inventory/batches/restore`. */
data class RestoreBatchResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("batchId") val batchId: String?,
    @SerializedName("restoredQuantity") val restoredQuantity: BigDecimal?
)
