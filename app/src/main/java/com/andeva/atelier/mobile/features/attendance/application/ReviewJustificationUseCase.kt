package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import javax.inject.Inject

/**
 * Aprueba o rechaza la justificación de tardanza/inasistencia de un colaborador (`hr:justifications:approve`, US10).
 */
class ReviewJustificationUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke(id: AttendanceId, approve: Boolean): Result<Attendance> =
        attendanceRepository.reviewJustification(id, approve)
}
