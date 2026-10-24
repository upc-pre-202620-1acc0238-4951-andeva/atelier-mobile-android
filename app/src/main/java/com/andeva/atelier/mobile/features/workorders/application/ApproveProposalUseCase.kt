package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.ProposalApproval
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/**
 * Registrar que el cliente aprobó la propuesta (US15 E1): el backend crea la tarea y recalcula el presupuesto.
 * Devuelve el detalle recargado. En línea.
 */
class ApproveProposalUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(workOrderId: String, proposalId: String, approval: ProposalApproval): Result<WorkOrderDetail> {
        val invalid = when {
            !WorkOrderValidation.isAmount(approval.finalPrice) -> "finalPrice"
            !WorkOrderValidation.isAmount(approval.laborHours) -> "laborHours"
            !WorkOrderValidation.isOptionalText(approval.notes, WorkOrderValidation.NOTES_MAX) -> "notes"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.approveProposal(workOrderId, proposalId, approval)
    }
}
