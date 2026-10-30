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
