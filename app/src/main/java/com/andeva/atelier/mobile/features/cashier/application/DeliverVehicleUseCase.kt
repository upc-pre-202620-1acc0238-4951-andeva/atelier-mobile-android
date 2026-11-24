package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.core.data.sync.WorkshopCacheEvent
import com.andeva.atelier.mobile.core.data.sync.WorkshopCacheEventBus
import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import javax.inject.Inject

/** Entrega el vehículo de una orden pagada y notifica la liberación de bahía al bus de caché. */
class DeliverVehicleUseCase @Inject constructor(
    private val cashierRepository: CashierRepository,
    private val cacheEventBus: WorkshopCacheEventBus? = null
) {
    suspend operator fun invoke(workOrderId: String): Result<WorkOrder> =
        cashierRepository.deliver(workOrderId)
            .onSuccess { cacheEventBus?.tryEmit(WorkshopCacheEvent.VehicleDelivered(workOrderId)) }
}
