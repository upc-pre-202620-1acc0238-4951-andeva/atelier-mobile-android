package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import javax.inject.Inject

/** Cancela una orden de compra pendiente antes de su recepción (`inventory:purchase_orders:create`). */
class CancelPurchaseOrderUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(id: String, reason: String? = null): Result<PurchaseOrder> {
        if (!InventoryValidation.isNonEmptyText(id)) {
            return Result.failure(InvalidInventoryInputException("id"))
        }
        return inventoryRepository.cancelPurchaseOrder(id, reason)
    }
}
