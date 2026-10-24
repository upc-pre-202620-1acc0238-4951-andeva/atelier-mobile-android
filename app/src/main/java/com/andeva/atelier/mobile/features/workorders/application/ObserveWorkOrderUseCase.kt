package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Cabecera de la orden desde Room: estado y totales con IGV sin red (US36). El detalle es [GetWorkOrderDetailUseCase]. */
class ObserveWorkOrderUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    operator fun invoke(id: String) = workOrderRepository.observeWorkOrder(id)
}
