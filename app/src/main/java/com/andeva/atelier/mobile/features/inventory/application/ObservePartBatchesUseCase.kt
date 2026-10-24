package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import javax.inject.Inject

/** Lotes del repuesto desde Room en orden FIFO (el más antiguo primero); `Batch.isActive` separa los agotados (US32). */
class ObservePartBatchesUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke(itemId: String) = inventoryRepository.observeBatches(itemId)
}
