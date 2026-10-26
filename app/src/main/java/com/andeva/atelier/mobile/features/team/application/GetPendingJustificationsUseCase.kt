package com.andeva.atelier.mobile.features.team.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.team.domain.PendingJustifications
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import javax.inject.Inject

/** Bandeja de descargos por revisar de los últimos [days] días de Lima (US10). Sin `audit_all`, vacía sin llamar. */
class GetPendingJustificationsUseCase @Inject constructor(
    private val teamRepository: TeamRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(days: Int = 7): Result<PendingJustifications> {
        if (sessionManager.getSession()?.has(Permission.ATTENDANCE_AUDIT_ALL) != true) {
            return Result.success(PendingJustifications(emptyList(), emptyList()))
        }
        return teamRepository.getPendingJustifications(days)
    }
}
