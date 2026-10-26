package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import javax.inject.Inject

/** Entrega el vehículo de una orden pagada. */
class DeliverVehicleUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(workOrderId: String): Result<WorkOrder> = cashierRepository.deliver(workOrderId)
}
