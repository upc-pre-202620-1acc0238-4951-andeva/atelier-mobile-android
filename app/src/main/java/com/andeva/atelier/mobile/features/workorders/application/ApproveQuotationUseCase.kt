package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Conformidad del cliente con la proforma (US25 E1): la orden pasa a `IN_PROGRESS`. En línea. */
class ApproveQuotationUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String): Result<WorkOrder> = workOrderRepository.approveQuotation(id)
}
