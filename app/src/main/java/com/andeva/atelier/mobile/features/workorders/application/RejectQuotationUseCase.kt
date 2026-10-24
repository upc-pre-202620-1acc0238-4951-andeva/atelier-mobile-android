package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/**
 * El cliente desiste (US25 E2): la orden queda `CANCELED`, se liberan sus materiales y sale de la lista de la sede.
 * En línea.
 */
class RejectQuotationUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String, reason: String): Result<WorkOrder> {
        if (!WorkOrderValidation.isRequiredText(reason, WorkOrderValidation.REASON_MAX)) {
            return Result.failure(InvalidWorkOrderInputException("reason"))
        }
        return workOrderRepository.rejectQuotation(id, reason)
    }
}
