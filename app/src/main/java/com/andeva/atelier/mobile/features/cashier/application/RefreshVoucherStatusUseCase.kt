package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import javax.inject.Inject

/** Consulta y refresca el estado fiscal de un comprobante ante SUNAT (`GET /invoicing/vouchers/{id}`). */
class RefreshVoucherStatusUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(voucherId: String): Result<Voucher> =
        cashierRepository.getVoucher(voucherId)
}
