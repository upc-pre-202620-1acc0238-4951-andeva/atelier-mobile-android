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
