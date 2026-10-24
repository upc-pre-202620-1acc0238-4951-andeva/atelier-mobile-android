package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import javax.inject.Inject

/**
 * Bandeja de alertas de reposición (US34 E1), destino del push `LOW_STOCK` (que puede repetirse, H14). Las alertas
 * no se guardan en el backend (H13): sin red se derivan de la caché con la misma regla.
 */
class GetStockAlertsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(): Result<List<StockAlert>> = inventoryRepository.getStockAlerts()
}
