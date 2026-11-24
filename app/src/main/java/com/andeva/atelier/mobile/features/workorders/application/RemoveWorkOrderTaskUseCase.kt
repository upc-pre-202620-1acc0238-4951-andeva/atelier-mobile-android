package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Elimina una tarea no completada de una orden de trabajo y devuelve el detalle con totales recalculados. En línea. */
class RemoveWorkOrderTaskUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(workOrderId: String, taskId: String): Result<WorkOrderDetail> =
        workOrderRepository.removeTask(workOrderId, taskId)
}
