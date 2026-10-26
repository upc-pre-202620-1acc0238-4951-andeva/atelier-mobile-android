package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.time.LocalDate

/** `DailyReconciliationResource` (docs/api/invoicing.md): lo cobrado en la sede un día, por forma de pago. */
data class DailyCashSummary(
    val date: LocalDate,
    val totalCollected: Money,
    val paymentCount: Int,
    val byMethod: Map<PaymentMethod, Money>,
    val payments: List<Payment>
)
