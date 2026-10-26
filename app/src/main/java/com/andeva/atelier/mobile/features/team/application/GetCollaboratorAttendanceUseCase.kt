package com.andeva.atelier.mobile.features.team.application

import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import javax.inject.Inject

/**
 * Destino del push `OVERTIME_RECORDED` (`PushDestination.CollaboratorAttendance`). El historial admite
 * `audit_all` o `read_own`, así que no se filtra aquí. Devuelve null si el registro no está en los últimos 30 días.
 */
class GetCollaboratorAttendanceUseCase @Inject constructor(
    private val teamRepository: TeamRepository
) {
    suspend operator fun invoke(membershipId: String, attendanceId: String): Result<TeamAttendance?> =
        teamRepository.getCollaboratorAttendance(membershipId, attendanceId)
}
