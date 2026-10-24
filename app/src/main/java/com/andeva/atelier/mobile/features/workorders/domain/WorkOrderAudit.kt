package com.andeva.atelier.mobile.features.workorders.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.time.Instant

/**
 * Descargo de un repuesto desde un lote y lo que volvió a él (`PartMovementResource`, US33 E2). [lastReturnedBy]
 * es el id del usuario que lo retiró.
 */
data class PartMovement(
    val allocationId: String,
    val productId: String,
    val taskId: String?,
    val batchId: String?,
    val chargedQuantity: BigDecimal,
    val returnedQuantity: BigDecimal,
    val unitCost: Money?,
    val chargedAt: Instant?,
    val lastReturnedAt: Instant?,
    val lastReturnedBy: String?,
    val lastReturnReason: String?
)

/**
 * Rentabilidad de la orden con costo de ventas FIFO (`WorkOrderProfitabilityResource`, US35 E2). Montos en
 * [currency] y sin IGV; la ganancia puede ser negativa, por eso no se usa [Money] (que exige ≥ 0).
 */
data class Profitability(
    val workOrderId: String,
    val currency: String?,
    val laborRevenue: BigDecimal?,
    val partsRevenue: BigDecimal?,
    val partsCostOfSales: BigDecimal?,
    val partsGrossProfit: BigDecimal?,
    val subtotal: BigDecimal?,
    val lines: List<PartProfitability>
)

/** Una línea de repuesto de [Profitability], en la misma moneda. */
data class PartProfitability(
    val taskId: String?,
    val productId: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal?,
    val revenue: BigDecimal?,
    val costOfSales: BigDecimal?,
    val grossProfit: BigDecimal?
)
