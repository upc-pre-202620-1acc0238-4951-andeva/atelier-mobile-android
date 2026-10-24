package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Al abrir la lista de órdenes y al tocar `WORK_ORDER_COMPLETED`: recarga las órdenes no cerradas de la sede.
 * Falla con `MissingBranchException` sin sede; sin red se sigue con la caché ("Actualizado hace N min").
 */
class RefreshBranchWorkOrdersUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(): Result<Unit> = workOrderRepository.refreshBranchWorkOrders()
}
