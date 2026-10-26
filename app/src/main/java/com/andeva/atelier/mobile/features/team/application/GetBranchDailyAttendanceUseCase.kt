package com.andeva.atelier.mobile.features.team.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import java.time.LocalDate
import javax.inject.Inject

/** Asistencia de la sede en un día de Lima (US10). Sin `hr:attendance:audit_all`, lista vacía sin llamar. En línea. */
class GetBranchDailyAttendanceUseCase @Inject constructor(
    private val teamRepository: TeamRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(date: LocalDate): Result<List<TeamAttendance>> {
        if (sessionManager.getSession()?.has(Permission.ATTENDANCE_AUDIT_ALL) != true) return Result.success(emptyList())
        return teamRepository.getBranchDaily(date)
    }
}
