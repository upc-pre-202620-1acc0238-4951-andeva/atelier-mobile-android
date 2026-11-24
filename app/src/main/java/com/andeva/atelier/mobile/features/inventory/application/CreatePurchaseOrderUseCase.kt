package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLineInput
import javax.inject.Inject

/** Creación de orden de compra en borrador (`inventory:purchase_orders:create`). */
class CreatePurchaseOrderUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        supplierId: String,
        items: List<PurchaseOrderLineInput> = emptyList(),
        orderNumber: String? = null,
        expectedDeliveryDate: String? = null,
        notes: String? = null
    ): Result<PurchaseOrder> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(supplierId) -> "supplierId"
            items.any { !InventoryValidation.isNonEmptyText(it.itemId) } -> "itemId"
            items.any { !InventoryValidation.isPositiveQty(it.quantity) } -> "quantity"
            items.any { !InventoryValidation.isNonNegative(it.unitCost) } -> "unitCost"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.createPurchaseOrder(supplierId, items, orderNumber, expectedDeliveryDate, notes)
    }
}
