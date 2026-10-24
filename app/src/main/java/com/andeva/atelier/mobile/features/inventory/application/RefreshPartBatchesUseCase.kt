package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import javax.inject.Inject

/** Recarga los lotes del repuesto si la caché tiene más de 1 h (US32). Sin `inventory:batches:read` no hace nada. */
class RefreshPartBatchesUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(itemId: String, force: Boolean = false): Result<Unit> = inventoryRepository.refreshBatches(itemId, force)
}
