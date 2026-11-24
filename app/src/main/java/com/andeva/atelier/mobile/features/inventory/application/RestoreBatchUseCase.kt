package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.BatchRestoreResult
import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import java.math.BigDecimal
import javax.inject.Inject

/** Reintegro de stock no usado al lote de origen (`inventory:batches:restore`). */
class RestoreBatchUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        itemId: String,
        quantity: BigDecimal,
        batchId: String? = null,
        reason: String? = null,
        workOrderId: String? = null
    ): Result<BatchRestoreResult> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(itemId) -> "itemId"
            !InventoryValidation.isPositiveQty(quantity) -> "quantity"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.restoreBatch(itemId, quantity, batchId, reason, workOrderId)
    }
}
