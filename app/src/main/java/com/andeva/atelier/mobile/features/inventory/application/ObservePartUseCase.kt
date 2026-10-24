package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import javax.inject.Inject

/** Ficha del repuesto desde la caché `parts` (sin red). */
class ObservePartUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke(id: String) = inventoryRepository.observePart(id)
}
