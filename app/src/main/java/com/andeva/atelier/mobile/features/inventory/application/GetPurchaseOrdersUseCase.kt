package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import javax.inject.Inject

/** Órdenes de compra por estado (por defecto las emitidas, las que se pueden recibir: US34 E2). En línea. */
class GetPurchaseOrdersUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(status: PurchaseOrderStatus? = PurchaseOrderStatus.ISSUED): Result<List<PurchaseOrder>> =
        inventoryRepository.getPurchaseOrders(status)
}
