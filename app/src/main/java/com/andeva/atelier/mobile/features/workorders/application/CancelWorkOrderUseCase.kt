package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/** Cancela la orden (`operations:work_orders:cancel`) con un motivo de 1 a 1000 caracteres. En línea. */
class CancelWorkOrderUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String, reason: String): Result<WorkOrder> {
        if (!WorkOrderValidation.isRequiredText(reason, WorkOrderValidation.REASON_MAX)) {
            return Result.failure(InvalidWorkOrderInputException("reason"))
        }
        return workOrderRepository.cancel(id, reason)
    }
}
