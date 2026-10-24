package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.PartDetail
import javax.inject.Inject

/**
 * Repuesto con sus lotes en orden FIFO (US31, US32): de qué lote saldrá el próximo descargo. Lotes solo con
 * `inventory:batches:read`. Destino del push `LOW_STOCK` (`itemId`). Sin red, desde la caché.
 */
class GetPartDetailUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(id: String): Result<PartDetail> = inventoryRepository.getPartDetail(id)
}
