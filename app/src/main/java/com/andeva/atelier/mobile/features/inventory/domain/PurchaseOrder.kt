package com.andeva.atelier.mobile.features.inventory.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.time.Instant

/** `PurchaseOrderStatus` de docs/api/README.md §8. Solo `ISSUED` se puede recibir. */
enum class PurchaseOrderStatus { DRAFT, ISSUED, RECEIVED, CANCELED }

/** Orden de compra a proveedor (`PurchaseOrderResource`). Se recibe para reponer stock (US34 E2). */
data class PurchaseOrder(
    val id: String,
    val orderNumber: String?,
    val supplierId: String?,
    val branchId: String?,
    val status: PurchaseOrderStatus?,
    val totalCost: Money?,
    val receiptNumber: String?,
    val receiptImageUrl: String?,
    val receivedAt: Instant?,
    val lines: List<PurchaseOrderLine>
) {
    val canBeReceived: Boolean get() = status == PurchaseOrderStatus.ISSUED
}

/** Línea de la orden: al recibir, cada una crea un lote con su cantidad y costo. */
data class PurchaseOrderLine(
    val id: String,
    val itemId: String,
    val quantity: BigDecimal,
    val unitCost: Money?,
    val totalCost: Money?
)

/** Valorización del inventario: saldo de los lotes por su costo, en PEN. */
data class InventoryValuation(val total: Money)
