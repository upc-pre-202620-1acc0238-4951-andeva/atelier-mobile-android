package com.andeva.atelier.mobile.features.inventory.infrastructure.local

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLine
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.BatchDto
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.PartDto
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.PurchaseOrderDto
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.StockAlertDto
import java.math.BigDecimal
import java.time.Instant

/** Room guarda montos y cantidades como texto decimal (docs/06-offline-sync.md §5); lo que falta queda en "". */
fun PartDto.toEntity(syncedAt: Long) = PartEntity(
    id = id,
    name = name.orEmpty(),
    sku = sku.orEmpty(),
    category = category.orEmpty(),
    basePrice = basePrice?.toPlainString().orEmpty(),
    totalStock = (totalStock ?: BigDecimal.ZERO).toPlainString(),
    minimumStock = (minimumStock ?: BigDecimal.ZERO).toPlainString(),
    unitOfMeasure = unitOfMeasure.orEmpty(),
    status = status.orEmpty(),
    syncedAt = syncedAt
)

/** Los precios del catálogo están en PEN (docs/api/README.md §6). */
fun PartEntity.toDomain() = Part(
    id = id,
    name = name,
    sku = sku,
    category = category.toEnum<ItemCategory>(),
    basePrice = money(basePrice.toBigDecimalOrNull()),
    totalStock = totalStock.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    minimumStock = minimumStock.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    unitOfMeasure = unitOfMeasure.ifEmpty { null },
    status = status.toEnum<PartStatus>()
)

/** Sin `itemId` en la respuesta se usa el repuesto consultado, para que caiga en su alcance `batches:{itemId}`. */
fun BatchDto.toEntity(fallbackItemId: String, syncedAt: Long) = BatchEntity(
    id = id,
    itemId = itemId ?: fallbackItemId,
    batchNumber = batchNumber.orEmpty(),
    remainingQty = (remainingQty ?: BigDecimal.ZERO).toPlainString(),
    unitCost = (unitCost ?: BigDecimal.ZERO).toPlainString(),
    arrivalDate = arrivalDate.toInstant()?.toEpochMilli() ?: 0,
    syncedAt = syncedAt
)

fun BatchEntity.toDomain() = Batch(
    id = id,
    itemId = itemId,
    batchNumber = batchNumber,
    remainingQty = remainingQty.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    unitCost = money(unitCost.toBigDecimalOrNull()),
    arrivalDate = arrivalDate.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
)

/** En línea llegan también los campos que la caché no guarda. */
fun BatchDto.toDomain(fallbackItemId: String) = Batch(
    id = id,
    itemId = itemId ?: fallbackItemId,
    batchNumber = batchNumber.orEmpty(),
    remainingQty = remainingQty ?: BigDecimal.ZERO,
    unitCost = money(unitCost),
    arrivalDate = arrivalDate.toInstant(),
    initialQty = initialQty,
    supplierId = supplierId,
    purchaseOrderId = purchaseOrderId,
    receiptImageUrl = receiptImageUrl
)

fun StockAlertDto.toDomain() = StockAlert(
    itemId = itemId,
    name = name.orEmpty(),
    sku = sku.orEmpty(),
    category = category.toEnum<ItemCategory>(),
    currentStock = currentStock ?: BigDecimal.ZERO,
    minimumStock = minimumStock ?: BigDecimal.ZERO,
    deficit = deficit ?: BigDecimal.ZERO
)

fun PurchaseOrderDto.toDomain() = PurchaseOrder(
    id = id,
    orderNumber = orderNumber,
    supplierId = supplierId,
    branchId = branchId,
    status = status.toEnum<PurchaseOrderStatus>(),
    totalCost = money(totalCost),
    receiptNumber = receiptNumber,
    receiptImageUrl = receiptImageUrl,
    receivedAt = receivedAt.toInstant(),
    lines = items.orEmpty().map {
        PurchaseOrderLine(it.id, it.itemId, it.quantity ?: BigDecimal.ZERO, money(it.unitCost), money(it.totalCost))
    }
)

private inline fun <reified T : Enum<T>> String?.toEnum(): T? = enumValues<T>().firstOrNull { it.name == this }

private fun String?.toInstant(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

/** El inventario se valoriza en PEN; null si falta el monto o es negativo (Money no lo admite). */
internal fun money(amount: BigDecimal?): Money? = amount?.let { runCatching { Money(it, Money.PEN) }.getOrNull() }
