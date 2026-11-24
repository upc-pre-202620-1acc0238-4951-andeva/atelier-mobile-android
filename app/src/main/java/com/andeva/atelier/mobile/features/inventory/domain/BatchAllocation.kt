package com.andeva.atelier.mobile.features.inventory.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal

/** Resultado de un descargo FIFO (`StockAllocationResource` de docs/api/inventory.md). */
data class StockAllocation(
    val allocationId: String,
    val allocatedQuantity: BigDecimal,
    val totalCogs: Money?,
    val deductions: List<BatchDeduction>
)

/** Deducción individual sobre un lote dentro de un descargo FIFO (`BatchDeductionResource`). */
data class BatchDeduction(
    val batchId: String,
    val quantityDeducted: BigDecimal,
    val unitCost: Money?,
    val subtotal: Money?
)

/** Respuesta de `POST /api/v1/inventory/batches/restore`. */
data class BatchRestoreResult(
    val status: String?,
    val message: String?,
    val batchId: String?,
    val restoredQuantity: BigDecimal
)
