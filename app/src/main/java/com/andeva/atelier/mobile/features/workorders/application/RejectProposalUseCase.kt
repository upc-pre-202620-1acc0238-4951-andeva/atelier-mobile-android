package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/** El cliente no autoriza la propuesta (US15 E2): queda rechazada con sus observaciones; la orden no cambia. En línea. */
class RejectProposalUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(workOrderId: String, proposalId: String, customerNotes: String): Result<Proposal> {
        if (!WorkOrderValidation.isRequiredText(customerNotes, WorkOrderValidation.NOTES_MAX)) {
            return Result.failure(InvalidWorkOrderInputException("customerNotes"))
        }
        return workOrderRepository.rejectProposal(workOrderId, proposalId, customerNotes)
    }
}
