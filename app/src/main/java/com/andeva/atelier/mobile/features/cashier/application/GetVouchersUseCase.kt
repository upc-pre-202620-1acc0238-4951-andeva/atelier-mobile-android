package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import java.time.LocalDate
import javax.inject.Inject

/** Lista los comprobantes electrónicos emitidos de la sede (`invoicing:invoices:read`). */
class GetVouchersUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(
        type: VoucherType? = null,
        status: VoucherStatus? = null,
        from: LocalDate? = null,
        to: LocalDate? = null
    ): Result<List<Voucher>> = cashierRepository.getVouchers(type, status, from, to)
}
