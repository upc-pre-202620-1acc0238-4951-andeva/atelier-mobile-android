package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.Payment
import javax.inject.Inject

/** Historial de cobros del comprobante, leído en la red. */
class GetVoucherPaymentsUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(voucherId: String): Result<List<Payment>> = cashierRepository.getPayments(voucherId)
}
