package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import java.math.BigDecimal
import javax.inject.Inject

/** Agrega una línea a una orden de compra en borrador (`inventory:purchase_orders:create`). */
class AddPurchaseOrderItemUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        purchaseOrderId: String,
        itemId: String,
        quantity: BigDecimal,
        unitCost: BigDecimal
    ): Result<PurchaseOrder> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(purchaseOrderId) -> "purchaseOrderId"
            !InventoryValidation.isNonEmptyText(itemId) -> "itemId"
            !InventoryValidation.isPositiveQty(quantity) -> "quantity"
            !InventoryValidation.isNonNegative(unitCost) -> "unitCost"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.addPurchaseOrderItem(purchaseOrderId, itemId, quantity, unitCost)
    }
}
