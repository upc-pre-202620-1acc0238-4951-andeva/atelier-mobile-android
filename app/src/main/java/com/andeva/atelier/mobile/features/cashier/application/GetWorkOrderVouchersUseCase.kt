package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import javax.inject.Inject

/** Comprobantes de la orden leídos en la red; deja sus cabeceras en la caché. */
class GetWorkOrderVouchersUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(workOrderId: String): Result<List<Voucher>> =
        cashierRepository.refreshWorkOrderVouchers(workOrderId)
}
