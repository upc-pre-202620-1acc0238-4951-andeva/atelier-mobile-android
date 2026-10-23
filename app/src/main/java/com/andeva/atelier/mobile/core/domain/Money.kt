package com.andeva.atelier.mobile.core.domain

import java.math.BigDecimal

data class Money(
    val amount: BigDecimal,
    val currency: String
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount must be greater than or equal to 0" }
        require(currency == PEN || currency == USD) { "Currency must be PEN or USD" }
    }

    companion object {
        const val PEN = "PEN"
        const val USD = "USD"

        /** El backend envía números y Room guarda texto decimal (docs/06-offline-sync.md §5). */
        fun of(amount: String, currency: String) = Money(BigDecimal(amount), currency)
    }
}
