package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Ingreso físico directo de lote (`inventory:batches:receive`). En línea: sin red falla y no se encola. */
class ReceiveDirectBatchUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        itemId: String,
        batchNumber: String,
        quantity: BigDecimal,
        unitCost: BigDecimal,
        arrivalDate: Instant? = null,
        supplierId: String? = null
    ): Result<Batch> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(itemId) -> "itemId"
            !InventoryValidation.isNonEmptyText(batchNumber) -> "batchNumber"
            !InventoryValidation.isPositiveQty(quantity) -> "quantity"
            !InventoryValidation.isNonNegative(unitCost) -> "unitCost"
            !InventoryValidation.isReceivedAt(arrivalDate, clock.instant()) -> "arrivalDate"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.receiveDirectBatch(itemId, batchNumber, quantity, unitCost, arrivalDate, supplierId)
    }
}
