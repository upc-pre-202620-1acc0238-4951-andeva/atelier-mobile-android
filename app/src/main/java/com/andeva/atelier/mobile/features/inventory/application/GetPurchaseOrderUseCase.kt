package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import javax.inject.Inject

/** Orden de compra con sus líneas, para revisar antes de recibirla (US34 E2). En línea. */
class GetPurchaseOrderUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(id: String): Result<PurchaseOrder> = inventoryRepository.getPurchaseOrder(id)
}
