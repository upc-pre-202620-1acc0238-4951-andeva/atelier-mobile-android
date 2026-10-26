package com.andeva.atelier.mobile.features.team.application

import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import javax.inject.Inject

/** Aprueba un descargo (queda `EXCUSED`). No se filtra: la UI lo oculta sin `JUSTIFICATIONS_APPROVE` y el backend responde 403. */
class ApproveJustificationUseCase @Inject constructor(
    private val teamRepository: TeamRepository
) {
    suspend operator fun invoke(id: String): Result<TeamAttendance> = teamRepository.approveJustification(id)
}
