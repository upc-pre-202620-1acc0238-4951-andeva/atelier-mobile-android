package com.andeva.atelier.mobile.features.cashier.presentation

import androidx.annotation.StringRes
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.MissingFiscalSeriesException
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.PaymentOutcomeUnknownException
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import java.math.BigDecimal

@StringRes
internal fun PaymentMethod?.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.cashier_method_cash
    PaymentMethod.CREDIT_CARD -> R.string.cashier_method_credit_card
    PaymentMethod.DEBIT_CARD -> R.string.cashier_method_debit_card
    PaymentMethod.BANK_TRANSFER -> R.string.cashier_method_bank_transfer
    PaymentMethod.DIGITAL_WALLET_YAPE -> R.string.cashier_method_yape
    PaymentMethod.DIGITAL_WALLET_PLIN -> R.string.cashier_method_plin
    null -> R.string.cashier_method_unknown
}

@StringRes
internal fun VoucherType?.labelRes(): Int = when (this) {
    VoucherType.FACTURA -> R.string.cashier_type_factura
    VoucherType.BOLETA -> R.string.cashier_type_boleta
    VoucherType.NOTA_CREDITO -> R.string.cashier_type_credit_note
    null -> R.string.cashier_type_unknown
}

@StringRes
internal fun FiscalDocumentType?.labelRes(): Int = when (this) {
    FiscalDocumentType.RUC -> R.string.cashier_doc_ruc
    FiscalDocumentType.DNI -> R.string.cashier_doc_dni
    FiscalDocumentType.CE -> R.string.cashier_doc_ce
    FiscalDocumentType.PASAPORTE -> R.string.cashier_doc_passport
    null -> R.string.cashier_type_unknown
}

/** Texto y tipo de marca del estado fiscal del comprobante. */
internal fun VoucherStatus?.chip(): Pair<Int, ChipKind> = when (this) {
    VoucherStatus.DRAFT -> R.string.cashier_vstatus_draft to ChipKind.Idle
    VoucherStatus.ISSUED -> R.string.cashier_vstatus_issued to ChipKind.Info
    VoucherStatus.ACCEPTED_SUNAT -> R.string.cashier_vstatus_accepted to ChipKind.Ok
    VoucherStatus.REJECTED_SUNAT -> R.string.cashier_vstatus_rejected to ChipKind.Bad
    VoucherStatus.VOIDED -> R.string.cashier_vstatus_voided to ChipKind.Bad
    null -> R.string.cashier_vstatus_unknown to ChipKind.Idle
}

/** Texto y tipo de marca del estado de cobro del comprobante. */
internal fun VoucherPaymentStatus?.chip(): Pair<Int, ChipKind> = when (this) {
    VoucherPaymentStatus.PAID -> R.string.cashier_pstatus_paid to ChipKind.Ok
    VoucherPaymentStatus.PARTIAL -> R.string.cashier_pstatus_partial to ChipKind.Info
    VoucherPaymentStatus.PENDING, null -> R.string.cashier_pstatus_pending to ChipKind.Idle
}

/** Importe con símbolo; acepta la moneda como la envió el backend (PEN si falta o no es USD) y montos negativos. */
internal fun formatAmount(amount: BigDecimal, currency: String?): String {
    val money = Money(amount.abs(), if (currency == Money.USD) Money.USD else Money.PEN).format()
    return if (amount.signum() < 0) "−$money" else money
}

/** Errores propios de caja con su texto; el resto, el `message` del backend (AGENTS.md). */
internal fun Throwable.toCashierText(): UiText = when (this) {
    is InvalidCashierInputException -> UiText.Resource(
        when (field) {
            "lines" -> R.string.cashier_err_lines
            "taxId" -> R.string.cashier_err_tax_id
            "legalName" -> R.string.cashier_err_legal_name
            "tenantName", "tenantLegalName" -> R.string.cashier_err_tenant_name
            "serie" -> R.string.cashier_err_serie
            "initialCorrelative" -> R.string.cashier_err_initial
            "amount" -> R.string.cashier_err_amount
            "transactionReference" -> R.string.cashier_err_reference
            "status" -> R.string.cashier_err_status
            "reason" -> R.string.cashier_err_reason
            "customerId" -> R.string.cashier_err_customer
            else -> R.string.cashier_err_input
        }
    )
    is MissingFiscalSeriesException -> UiText.Resource(R.string.cashier_err_no_series)
    is PaymentOutcomeUnknownException -> UiText.Resource(R.string.cashier_err_outcome_unknown)
    is MissingBranchException -> UiText.Resource(R.string.cashier_err_no_branch)
    else -> toUiText()
}
