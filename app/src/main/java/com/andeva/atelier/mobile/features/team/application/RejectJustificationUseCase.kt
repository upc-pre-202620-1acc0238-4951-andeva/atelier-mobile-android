package com.andeva.atelier.mobile.features.team.application

import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import javax.inject.Inject

/** Rechaza un descargo. No se filtra: la UI lo oculta sin `JUSTIFICATIONS_APPROVE` y el backend responde 403. */
class RejectJustificationUseCase @Inject constructor(
    private val teamRepository: TeamRepository
) {
    suspend operator fun invoke(id: String): Result<TeamAttendance> = teamRepository.rejectJustification(id)
}
