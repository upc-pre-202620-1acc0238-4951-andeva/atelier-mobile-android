package com.andeva.atelier.mobile.features.inventory.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.time.Instant

/** `ItemCategory` de docs/api/README.md §8. */
enum class ItemCategory { LUBRICANTS, BRAKES, SUSPENSION, ENGINE, ELECTRICAL, TIRES, FILTERS, BODYWORK, ACCESSORIES }

/** `InventoryItemStatus` del backend. */
enum class PartStatus { ACTIVE, INACTIVE, DISCONTINUED }

/**
 * Repuesto del catálogo (`PartResource`, US31). [basePrice] es el precio de venta sin IGV. [unitOfMeasure] vale
 * siempre "UNIT" (H12 de docs/03-traceability.md).
 */
data class Part(
    val id: String,
    val name: String,
    val sku: String,
    val category: ItemCategory?,
    val basePrice: Money?,
    val totalStock: BigDecimal,
    val minimumStock: BigDecimal,
    val unitOfMeasure: String?,
    val status: PartStatus?
) {
    /** Existencia agotada (US31 E2): la UI la resalta y bloquea su selección. */
    val isOutOfStock: Boolean get() = totalStock <= BigDecimal.ZERO

    /** Misma regla que `GET /inventory/alerts`: en el mínimo o debajo (US34 E1). */
    val isLowStock: Boolean get() = totalStock <= minimumStock

    /** Se puede elegir para una tarea: activo y con saldo. */
    val isSelectable: Boolean get() = status != PartStatus.INACTIVE && status != PartStatus.DISCONTINUED && !isOutOfStock

    /** Alerta derivada de la caché con la misma regla del backend (H13 de docs/03-traceability.md). */
    fun toStockAlert() = StockAlert(
        itemId = id,
        name = name,
        sku = sku,
        category = category,
        currentStock = totalStock,
        minimumStock = minimumStock,
        deficit = (minimumStock - totalStock).max(BigDecimal.ZERO)
    )
}

/**
 * Lote de compra (`BatchResource`, US32). [initialQty], [supplierId], [purchaseOrderId] y [receiptImageUrl] solo
 * llegan en línea: la caché `batches` no los guarda.
 */
data class Batch(
    val id: String,
    val itemId: String,
    val batchNumber: String,
    val remainingQty: BigDecimal,
    val unitCost: Money?,
    val arrivalDate: Instant?,
    val initialQty: BigDecimal? = null,
    val supplierId: String? = null,
    val purchaseOrderId: String? = null,
    val receiptImageUrl: String? = null
) {
    /** Con saldo: de estos sale el próximo descargo FIFO. */
    val isActive: Boolean get() = remainingQty > BigDecimal.ZERO
}

/** Detalle del repuesto con sus lotes en orden FIFO (el más antiguo primero). */
data class PartDetail(
    val part: Part,
    val batches: List<Batch>
)

/** Repuesto en el mínimo o debajo (`StockAlertResource`, US34). [deficit] es lo que falta para el mínimo. */
data class StockAlert(
    val itemId: String,
    val name: String,
    val sku: String,
    val category: ItemCategory?,
    val currentStock: BigDecimal,
    val minimumStock: BigDecimal,
    val deficit: BigDecimal
)
