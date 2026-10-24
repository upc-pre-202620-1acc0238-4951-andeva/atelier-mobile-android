package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import java.io.File
import javax.inject.Inject

/**
 * Proponer una tarea adicional por un hallazgo en foso, con severidad y foto (US14 E1). Sin foto no se envía (E2):
 * falla con [InvalidWorkOrderInputException] `photo`. `HIGH` no se puede enviar (docs/api/README.md §8). Funciona sin
 * red: se encola y el backend avisa a supervisión al sincronizar. El mecánico es el de la sesión.
 */
class SubmitProposalUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(
        id: String,
        description: String,
        severity: ProposalSeverity,
        photo: File?,
        suggestedServiceId: String? = null
    ): Result<Unit> {
        if (photo == null) return Result.failure(InvalidWorkOrderInputException("photo"))
        val invalid = when {
            !severity.isSubmittable -> "severity"
            !WorkOrderValidation.isRequiredText(description, WorkOrderValidation.PROPOSAL_DESCRIPTION_MAX) -> "description"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.submitProposal(id, description, severity, photo, suggestedServiceId)
    }
}
