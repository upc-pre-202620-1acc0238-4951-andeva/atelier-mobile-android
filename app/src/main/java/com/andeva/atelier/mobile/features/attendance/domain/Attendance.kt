package com.andeva.atelier.mobile.features.attendance.domain

import java.time.Instant

/** Una jornada (`AttendanceResource` de docs/api/hr.md). Los minutos los calcula el backend al marcar la salida. */
data class Attendance(
    val id: AttendanceId,
    val branchId: String,
    val shiftId: String,
    val clockIn: Instant,
    val clockOut: Instant?,
    val status: AttendanceStatus?,                    // null si el backend envía un estado nuevo
    val distanceToBranchMeters: Double,
    val workedMinutes: Long?,
    val regularMinutes: Long?,
    val overtimeMinutes: Long?,
    val justificationReason: String?,
    val justificationStatus: JustificationStatus?
) {
    val isOpen: Boolean get() = clockOut == null
}

/** Registro diario de asistencia de un colaborador de la sede (`GET /api/v1/hr/attendances/branch/{branchId}/daily`). */
data class BranchAttendanceItem(
    val attendance: Attendance,
    val membershipId: String?,
    val collaboratorLabel: String
)
