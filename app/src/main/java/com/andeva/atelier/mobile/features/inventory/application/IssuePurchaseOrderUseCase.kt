package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import javax.inject.Inject

/** Emite una orden de compra en borrador al proveedor (`inventory:purchase_orders:create`). */
class IssuePurchaseOrderUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(id: String): Result<PurchaseOrder> {
        if (!InventoryValidation.isNonEmptyText(id)) {
            return Result.failure(InvalidInventoryInputException("id"))
        }
        return inventoryRepository.issuePurchaseOrder(id)
    }
}
