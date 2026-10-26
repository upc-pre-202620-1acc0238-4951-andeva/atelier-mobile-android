package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import javax.inject.Inject

/** Pago formal de la orden (mark-as-paid). */
class MarkWorkOrderPaidUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(workOrderId: String): Result<WorkOrder> = cashierRepository.markWorkOrderPaid(workOrderId)
}
