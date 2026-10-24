package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Órdenes no cerradas de la sede desde Room, la más reciente primero, con placa y cliente si están en caché. */
class ObserveBranchWorkOrdersUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    operator fun invoke() = workOrderRepository.observeBranchWorkOrders()
}
