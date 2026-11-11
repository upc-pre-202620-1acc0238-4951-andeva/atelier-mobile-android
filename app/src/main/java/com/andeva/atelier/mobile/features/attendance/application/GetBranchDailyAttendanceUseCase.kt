package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import java.time.LocalDate
import javax.inject.Inject

/**
 * Consulta la asistencia diaria del personal de la sede activa (`GET /api/v1/hr/attendances/branch/{branchId}/daily`,
 * permiso `hr:attendance:audit_all`).
 */
class GetBranchDailyAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke(date: LocalDate? = null): Result<List<BranchAttendanceItem>> =
        attendanceRepository.getBranchDailyAttendance(date)
}
