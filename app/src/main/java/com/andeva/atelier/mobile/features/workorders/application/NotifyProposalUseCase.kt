package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Enviar al cliente la propuesta con su foto por correo (US15 E1). Devuelve el correo de destino. En línea. */
class NotifyProposalUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(workOrderId: String, proposalId: String): Result<String> =
        workOrderRepository.notifyProposal(workOrderId, proposalId)
}
