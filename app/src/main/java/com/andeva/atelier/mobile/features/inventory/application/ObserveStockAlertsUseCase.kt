package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Alertas derivadas de la caché `parts` (US34 E1/E2): se actualizan solas cuando se recarga el catálogo, p. ej. al
 * recibir una compra la alerta sale de la bandeja.
 */
class ObserveStockAlertsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    operator fun invoke() = inventoryRepository.observeLowStock().map { parts -> parts.map { it.toStockAlert() } }
}
