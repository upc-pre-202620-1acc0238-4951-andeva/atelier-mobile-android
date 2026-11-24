package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.MissingFiscalSeriesException
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Emite una nota de crédito electrónica referenciando un comprobante vigente (`invoicing:invoices:issue_sunat`).
 * Valida motivo obligatorio (1..250 caracteres), monto positivo con hasta 2 decimales y sin exceder el total del
 * comprobante de referencia, y selecciona la serie activa de nota de crédito (`07`) de la sede.
 */
class IssueCreditNoteUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(
        referenceVoucherId: String,
        reason: String,
        amount: BigDecimal,
        serieOverride: String? = null
    ): Result<Voucher> {
        val trimmedReason = reason.trim()
        if (!CashierValidation.isValidReason(trimmedReason)) {
            return Result.failure(InvalidCashierInputException("reason"))
        }
        if (!CashierValidation.isValidMoneyAmount(amount)) {
            return Result.failure(InvalidCashierInputException("amount"))
        }

        val reference = cashierRepository.getVoucher(referenceVoucherId).getOrElse { return Result.failure(it) }
        if (!reference.isActiveSale || reference.status == VoucherStatus.DRAFT) {
            return Result.failure(InvalidCashierInputException("status"))
        }
        if (amount > reference.totalAmount.amount) {
            return Result.failure(InvalidCashierInputException("amount"))
        }
        if (reference.customerId == null) {
            return Result.failure(InvalidCashierInputException("customerId"))
        }

        val resolvedSerie = if (!serieOverride.isNullOrBlank()) {
            serieOverride.trim().uppercase()
        } else {
            val series = cashierRepository.getBranchSeries().getOrElse { return Result.failure(it) }
            val creditNoteSeries = series.filter { it.active && it.type == VoucherType.NOTA_CREDITO }
            val preferredPrefix = when (reference.type) {
                VoucherType.FACTURA -> 'F'
                VoucherType.BOLETA -> 'B'
                else -> null
            }
            val activeSeries = creditNoteSeries.firstOrNull {
                preferredPrefix != null && it.serie.startsWith(preferredPrefix, ignoreCase = true)
            } ?: creditNoteSeries.firstOrNull()
                ?: return Result.failure(MissingFiscalSeriesException())
            activeSeries.serie
        }

        return cashierRepository.issueCreditNote(
            referenceVoucher = reference,
            serie = resolvedSerie,
            reason = trimmedReason,
            amount = amount
        )
    }
}
