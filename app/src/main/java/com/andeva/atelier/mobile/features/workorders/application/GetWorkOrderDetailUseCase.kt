package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Detalle en línea (US13 E2, US22, US36): tareas con repuestos y evidencias, propuestas y fotos de recepción; deja
 * la cabecera al día en Room. Es el destino de `WORK_ORDER_COMPLETED` y del supervisor que revisa suspensiones (H5,
 * [WorkOrderDetail.tasksAwaitingSuspensionReview]). Volver a llamarlo tras imputar o retirar repuestos con `tasks`.
 */
class GetWorkOrderDetailUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String): Result<WorkOrderDetail> = workOrderRepository.getDetail(id)
}
