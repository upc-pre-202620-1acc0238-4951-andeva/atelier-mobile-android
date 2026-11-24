package com.andeva.atelier.mobile.features.cashier.presentation

import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import java.math.BigDecimal

/** Formulario compartido para anular un comprobante electrónico (comunicación de baja). */
data class VoidForm(
    val voucherId: String,
    val voucherLabel: String,
    val reason: String = ""
) {
    val reasonValid: Boolean get() = CashierValidation.isValidReason(reason)
    val canSubmit: Boolean get() = reasonValid
}

/** Formulario compartido para emitir una nota de crédito referenciando un comprobante vigente. */
data class CreditNoteForm(
    val referenceVoucherId: String,
    val referenceLabel: String,
    val maxAmount: BigDecimal,
    val currency: String?,
    val reason: String = "",
    val amount: String = "",
    val availableSeries: List<FiscalSeries> = emptyList(),
    val selectedSerie: String = ""
) {
    val reasonValid: Boolean get() = CashierValidation.isValidReason(reason)
    val serieValid: Boolean get() = selectedSerie.isEmpty() || CashierValidation.isCreditNoteSeries(selectedSerie)

    val parsedAmount: BigDecimal?
        get() = amount.trim().replace(',', '.').toBigDecimalOrNull()?.takeIf {
            CashierValidation.isValidMoneyAmount(it, max = maxAmount)
        }

    val canSubmit: Boolean get() = reasonValid && parsedAmount != null && serieValid
}
