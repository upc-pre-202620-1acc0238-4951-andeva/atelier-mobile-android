package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/**
 * Agregar una tarea con su servicio, mecánico (opcional, H11) y precio sin IGV (US12 E2, US22 E1); el mecánico recibe
 * `TASK_ASSIGNED`. Devuelve el detalle recargado con los totales recalculados. En línea.
 */
class AddWorkOrderTaskUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String, task: NewWorkOrderTask): Result<WorkOrderDetail> {
        val invalid = when {
            !WorkOrderValidation.isRequiredText(task.description, WorkOrderValidation.TASK_DESCRIPTION_MAX) -> "description"
            !WorkOrderValidation.isOptionalAmount(task.price) -> "price"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.addTask(id, task)
    }
}
