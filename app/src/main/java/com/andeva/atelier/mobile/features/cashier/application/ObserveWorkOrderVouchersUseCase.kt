package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.VoucherSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Cabeceras en caché de los comprobantes de la orden (sin red). */
class ObserveWorkOrderVouchersUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    operator fun invoke(workOrderId: String): Flow<List<VoucherSummary>> =
        cashierRepository.observeWorkOrderVouchers(workOrderId)
}
