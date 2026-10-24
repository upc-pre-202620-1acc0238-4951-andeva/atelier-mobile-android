package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import javax.inject.Inject

/**
 * Al abrir el catálogo (US31): recarga todos los repuestos si la caché tiene más de 1 h ([force] para "deslizar y
 * recargar"). Sin red falla y se sigue con la caché ("Actualizado hace N min").
 */
class RefreshPartsCatalogUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<Unit> = inventoryRepository.refreshCatalog(force)
}
