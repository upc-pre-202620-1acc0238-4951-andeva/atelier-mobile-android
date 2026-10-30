package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import javax.inject.Inject

/**
 * Buscar por nombre comercial o SKU, con categoría opcional (US31 E1): nombre, SKU, categoría, precio y stock.
 * Sin red busca en la caché.
 */
class SearchPartsCatalogUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(query: String, category: ItemCategory? = null, brand: String? = null): Result<List<Part>> =
        inventoryRepository.searchParts(query, category, brand)
}
