package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderTaskChanges
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/** Cambiar descripción, mecánico o precio de una tarea (US22 E2); devuelve el detalle con los totales recalculados. En línea. */
class UpdateWorkOrderTaskUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(workOrderId: String, taskId: String, changes: WorkOrderTaskChanges): Result<WorkOrderDetail> {
        val invalid = when {
            !WorkOrderValidation.isOptionalText(changes.description, WorkOrderValidation.TASK_DESCRIPTION_MAX) -> "description"
            !WorkOrderValidation.isOptionalAmount(changes.price) -> "price"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.updateTask(workOrderId, taskId, changes)
    }
}
