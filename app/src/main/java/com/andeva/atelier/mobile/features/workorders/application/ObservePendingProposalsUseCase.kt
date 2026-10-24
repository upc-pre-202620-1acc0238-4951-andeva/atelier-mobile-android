package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Propuestas capturadas que esperan enviarse con su foto (US14), para mostrarlas como "pendientes" en la orden. */
class ObservePendingProposalsUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    operator fun invoke(id: String) = workOrderRepository.observePendingProposals(id)
}
