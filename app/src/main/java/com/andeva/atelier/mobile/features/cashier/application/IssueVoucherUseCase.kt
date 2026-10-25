package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.MissingFiscalSeriesException
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import javax.inject.Inject

/**
 * Emite el comprobante (US37). No llega al backend un borrador sin líneas o con una línea sin descripción real (vacía o
 * con el nombre genérico del detalle, H11: `lines`; la UI pide editarla), ni una factura sin RUC válido (`taxId`). Usa
 * la primera serie activa del tipo en la sede y, si no hay, falla con [MissingFiscalSeriesException].
 */
class IssueVoucherUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(draft: VoucherDraft): Result<IssueOutcome> {
        if (draft.lines.isEmpty() || draft.lines.any { it.needsDescription }) {
            return Result.failure(InvalidCashierInputException("lines"))
        }
        if (draft.type == VoucherType.FACTURA && !CashierValidation.isRuc(draft.customerInfo.taxId)) {
            return Result.failure(InvalidCashierInputException("taxId"))
        }
        val series = cashierRepository.getBranchSeries().getOrElse { return Result.failure(it) }
        val active = series.firstOrNull { it.active && it.type == draft.type }
            ?: return Result.failure(MissingFiscalSeriesException())
        return cashierRepository.issue(draft, active.serie)
    }
}
