package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.Part
import javax.inject.Inject

/**
 * Buscar por SKU exacto, p. ej. desde el lector de código de barras (US31 E1). null = no existe. Un SKU con formato
 * inválido falla con [InvalidInventoryInputException] `sku` sin llamar al backend. Sin red busca en la caché.
 */
class FindPartBySkuUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(sku: String): Result<Part?> {
        if (!InventoryValidation.isSku(sku)) return Result.failure(InvalidInventoryInputException("sku"))
        return inventoryRepository.findBySku(sku)
    }
}
