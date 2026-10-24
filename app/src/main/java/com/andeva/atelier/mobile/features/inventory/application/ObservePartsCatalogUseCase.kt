package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import javax.inject.Inject

/** Catálogo desde Room, por nombre y opcionalmente por categoría (US31). `Part.isOutOfStock` marca los agotados (E2). */
class ObservePartsCatalogUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke(category: ItemCategory? = null) = inventoryRepository.observeCatalog(category)
}
