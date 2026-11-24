package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import java.math.BigDecimal
import javax.inject.Inject

/** Actualización de repuesto (`inventory:parts:manage`). En línea: sin red falla con `IOException`. */
class UpdatePartUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        category: ItemCategory,
        basePrice: BigDecimal,
        minimumStock: BigDecimal,
        status: PartStatus? = null,
        unitOfMeasure: String? = null
    ): Result<Part> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(id) -> "id"
            !InventoryValidation.isNonEmptyText(name) -> "name"
            !InventoryValidation.isNonNegative(basePrice) -> "basePrice"
            !InventoryValidation.isNonNegative(minimumStock) -> "minimumStock"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.updatePart(id, name, category, basePrice, minimumStock, status, unitOfMeasure)
    }
}
