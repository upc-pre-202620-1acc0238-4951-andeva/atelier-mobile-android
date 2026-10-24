package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.Branch
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.domain.WorkShift
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.BranchDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.WorkShiftDto
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/** DTO → Entity. null si el backend no envió la hora de ingreso (no se puede ubicar en el día). */
fun AttendanceDto.toEntity(fallbackMembershipId: String, syncedAt: Long): AttendanceEntity? {
    val clockInMillis = clockIn?.let { Instant.parse(it).toEpochMilli() } ?: return null
    return AttendanceEntity(
        id = id,
        branchId = branchId.orEmpty(),
        membershipId = membershipId ?: fallbackMembershipId,
        shiftId = shiftId.orEmpty(),
        clockIn = clockInMillis,
        clockOut = clockOut?.let { Instant.parse(it).toEpochMilli() },
        status = status.orEmpty(),
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        distanceToBranchMeters = distanceToBranchMeters ?: 0.0,
        workedMinutes = workedMinutes,
        regularMinutes = regularMinutes,
        overtimeMinutes = overtimeMinutes,
        justificationReason = justificationReason,
        justificationStatus = justificationStatus,
        syncedAt = syncedAt
    )
}

fun AttendanceEntity.toDomain() = Attendance(
    id = AttendanceId(id),
    branchId = branchId,
    shiftId = shiftId,
    clockIn = Instant.ofEpochMilli(clockIn),
    clockOut = clockOut?.let { Instant.ofEpochMilli(it) },
    status = AttendanceStatus.entries.firstOrNull { it.name == status },
    distanceToBranchMeters = distanceToBranchMeters,
    workedMinutes = workedMinutes,
    regularMinutes = regularMinutes,
    overtimeMinutes = overtimeMinutes,
    justificationReason = justificationReason,
    justificationStatus = JustificationStatus.entries.firstOrNull { it.name == justificationStatus }
)

fun WorkShiftDto.toEntity(syncedAt: Long) = ShiftEntity(
    id = id,
    name = name.orEmpty(),
    startTime = startTime,
    endTime = endTime,
    gracePeriodMinutes = gracePeriodMinutes ?: 0,
    workDays = workDays.orEmpty().joinToString(","),
    restDays = restDays.orEmpty().joinToString(","),
    isActive = isActive ?: true,
    syncedAt = syncedAt
)

fun ShiftEntity.toDomain() = WorkShift(
    id = id,
    name = name,
    startTime = LocalTime.parse(startTime),
    endTime = LocalTime.parse(endTime),
    gracePeriodMinutes = gracePeriodMinutes,
    workDays = workDays.toDays(),
    restDays = restDays.toDays(),
    isActive = isActive
)

private fun String.toDays(): Set<DayOfWeek> =
    split(',').mapNotNull { day -> DayOfWeek.entries.firstOrNull { it.name == day.trim() } }.toSet()

/** null si la sede no tiene coordenadas (no sirve para la geocerca). */
fun BranchDto.toEntity(syncedAt: Long): BranchEntity? {
    return BranchEntity(
        id = id,
        name = name.orEmpty(),
        latitude = latitude ?: return null,
        longitude = longitude ?: return null,
        geofenceRadiusMeters = (geofenceRadiusMeters ?: return null).toDouble(),
        isActive = isActive ?: true,
        syncedAt = syncedAt
    )
}

fun BranchEntity.toDomain() = Branch(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    geofenceRadiusMeters = geofenceRadiusMeters,
    isActive = isActive
)
