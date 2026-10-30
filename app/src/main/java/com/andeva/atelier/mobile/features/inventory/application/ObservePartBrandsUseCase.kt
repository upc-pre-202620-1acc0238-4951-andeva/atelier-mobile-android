package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import javax.inject.Inject

/**
 * Marcas del catálogo guardado, para los filtros de US31. Vacío mientras el backend no envíe la marca (H35): la UI
 * no muestra el filtro y no inventa marcas.
 */
class ObservePartBrandsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke() = inventoryRepository.observeBrands()
}
