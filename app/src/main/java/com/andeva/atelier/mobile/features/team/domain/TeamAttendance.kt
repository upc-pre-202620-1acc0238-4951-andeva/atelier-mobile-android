package com.andeva.atelier.mobile.features.team.domain

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import java.time.Instant

/**
 * Jornada de un colaborador vista por quien audita la sede (`AttendanceResource` de docs/api/hr.md).
 * [clockIn] es null en una falta `ABSENT`. [jobTitle] lo completa el repositorio desde `EmployeeService`.
 */
data class TeamAttendance(
    val id: String,
    val membershipId: String,
    val branchId: String,
    val shiftId: String?,
    val clockIn: Instant?,
    val clockOut: Instant?,
    val status: AttendanceStatus?,                    // null si el backend envía un estado nuevo
    val distanceToBranchMeters: Double?,
    val workedMinutes: Long?,
    val overtimeMinutes: Long?,
    val justificationReason: String?,
    val justificationStatus: JustificationStatus?,    // null si no hay descargo o el estado es nuevo
    val reviewedBy: String?,
    val reviewedAt: Instant?,
    val jobTitle: String? = null
) {
    /** El descargo espera la decisión de quien audita. */
    val awaitsReview: Boolean get() = justificationStatus == JustificationStatus.PENDING
}
