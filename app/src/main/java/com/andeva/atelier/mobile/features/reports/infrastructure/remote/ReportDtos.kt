package com.andeva.atelier.mobile.features.reports.infrastructure.remote

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.reports.domain.CashFlowMovement
import com.andeva.atelier.mobile.features.reports.domain.CashFlowSummary
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.Instant

/** `CashFlowSummaryResource` de docs/api/invoicing.md. */
data class CashFlowSummaryDto(
    @SerializedName("totalIncome") val totalIncome: BigDecimal?,
    @SerializedName("totalExpenses") val totalExpenses: BigDecimal?,
    @SerializedName("accountsReceivable") val accountsReceivable: BigDecimal?,
    @SerializedName("netCashFlow") val netCashFlow: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("movements") val movements: List<CashFlowMovementDto>?
)

/** `CashFlowMovementResource` de docs/api/invoicing.md. */
data class CashFlowMovementDto(
    @SerializedName("transactionId") val transactionId: String?,
    @SerializedName("movementDate") val movementDate: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("concept") val concept: String?,
    @SerializedName("referenceNumber") val referenceNumber: String?,
    @SerializedName("amount") val amount: BigDecimal?,
    @SerializedName("runningBalance") val runningBalance: BigDecimal?
)

/** Un importe o fecha que falta lanza `IllegalArgumentException`: nunca se inventa un 0. */
fun CashFlowSummaryDto.toDomain(): CashFlowSummary {
    // Sin moneda por defecto: `Money` rechaza lo que no sea PEN/USD.
    val known = requireNotNull(currency) { "Missing currency" }
    return CashFlowSummary(
        totalIncome = Money(requireNotNull(totalIncome) { "Missing totalIncome" }, known),
        totalExpenses = Money(requireNotNull(totalExpenses) { "Missing totalExpenses" }, known),
        accountsReceivable = Money(requireNotNull(accountsReceivable) { "Missing accountsReceivable" }, known),
        netCashFlow = requireNotNull(netCashFlow) { "Missing netCashFlow" },
        currency = known,
        movements = movements.orEmpty().map { it.toDomain() }
    )
}

private fun CashFlowMovementDto.toDomain() = CashFlowMovement(
    transactionId = requireNotNull(transactionId) { "Missing transactionId" },
    movementDate = Instant.parse(requireNotNull(movementDate) { "Missing movementDate" }),
    type = type.orEmpty(),
    category = category.orEmpty(),
    concept = concept.orEmpty(),
    referenceNumber = referenceNumber.orEmpty(),
    amount = requireNotNull(amount) { "Missing amount" },
    runningBalance = requireNotNull(runningBalance) { "Missing runningBalance" }
)
