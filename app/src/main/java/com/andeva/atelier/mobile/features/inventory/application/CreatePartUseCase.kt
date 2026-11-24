package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import java.math.BigDecimal
import javax.inject.Inject

/** Alta de repuesto en catálogo (`inventory:parts:manage`). En línea: sin red falla con `IOException`. */
class CreatePartUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        name: String,
        sku: String,
        category: ItemCategory,
        basePrice: BigDecimal,
        minimumStock: BigDecimal,
        unitOfMeasure: String? = "UNIT"
    ): Result<Part> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(name) -> "name"
            !InventoryValidation.isSku(sku) -> "sku"
            !InventoryValidation.isNonNegative(basePrice) -> "basePrice"
            !InventoryValidation.isNonNegative(minimumStock) -> "minimumStock"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.createPart(name, sku, category, basePrice, minimumStock, unitOfMeasure)
    }
}
