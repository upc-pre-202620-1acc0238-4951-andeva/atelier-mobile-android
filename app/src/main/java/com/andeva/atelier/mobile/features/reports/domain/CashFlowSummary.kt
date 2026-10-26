package com.andeva.atelier.mobile.features.reports.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.time.Instant

/**
 * `CashFlowSummaryResource`. Los totales no son negativos ([Money]); el flujo neto, el importe y el saldo acumulado
 * de cada movimiento pueden serlo, por eso van como [BigDecimal] en la moneda [currency].
 */
data class CashFlowSummary(
    val totalIncome: Money,
    val totalExpenses: Money,
    val accountsReceivable: Money,
    val netCashFlow: BigDecimal,
    val currency: String,
    val movements: List<CashFlowMovement>
)

/** `CashFlowMovementResource`; `type` y `category` son textos del backend. */
data class CashFlowMovement(
    val transactionId: String,
    val movementDate: Instant,
    val type: String,
    val category: String,
    val concept: String,
    val referenceNumber: String,
    val amount: BigDecimal,
    val runningBalance: BigDecimal
)
