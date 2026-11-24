package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.StockAllocation
import java.math.BigDecimal
import javax.inject.Inject

/** Descargo contable FIFO directo de stock (`inventory:batches:dispatch_fifo`). */
class DispatchFifoBatchUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        itemId: String,
        quantity: BigDecimal,
        workOrderId: String? = null,
        taskId: String? = null
    ): Result<StockAllocation> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(itemId) -> "itemId"
            !InventoryValidation.isPositiveQty(quantity) -> "quantity"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.dispatchFifoBatch(itemId, quantity, workOrderId, taskId)
    }
}
