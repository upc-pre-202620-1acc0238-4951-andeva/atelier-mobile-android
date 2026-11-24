package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import javax.inject.Inject

/**
 * Anula un comprobante electrónico mediante comunicación de baja (`invoicing:invoices:issue_sunat`).
 * Exige motivo obligatorio (1..250 caracteres); la ventana de 7 días calendario la valida el backend.
 */
class VoidVoucherUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(voucherId: String, reason: String): Result<Voucher> {
        val trimmed = reason.trim()
        if (!CashierValidation.isValidReason(trimmed)) {
            return Result.failure(InvalidCashierInputException("reason"))
        }
        return cashierRepository.voidVoucher(voucherId, trimmed)
    }
}
