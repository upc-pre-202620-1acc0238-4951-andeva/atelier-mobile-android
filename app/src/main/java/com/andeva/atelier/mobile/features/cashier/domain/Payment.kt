package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.time.Instant

/** `VoucherPaymentResource` (docs/api/invoicing.md). */
data class Payment(
    val id: String,
    val voucherId: String?,
    val amount: Money,
    val method: PaymentMethod?,
    val transactionReference: String?,
    val status: String?,
    val paidAt: Instant?
)
