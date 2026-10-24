package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValuation
import javax.inject.Inject

/** Valorización del inventario: saldo de los lotes por su costo FIFO, en PEN (`inventory:batches:read`). En línea. */
class GetInventoryValuationUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(): Result<InventoryValuation> = inventoryRepository.getValuation()
}
