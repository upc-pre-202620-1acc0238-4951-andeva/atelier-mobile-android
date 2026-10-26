package com.andeva.atelier.mobile.features.team.infrastructure.remote

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Null si falta `id`, `membershipId` o `branchId`, o si una fecha presente no se puede leer:
 * nunca se inventa un dato. `clockIn` ausente es válido (falta `ABSENT`). Un estado desconocido queda en null.
 */
fun AttendanceDto.toTeamAttendanceOrNull(): TeamAttendance? {
    if (id.isBlank()) return null
    val member = membershipId?.takeIf { it.isNotBlank() } ?: return null
    val branch = branchId?.takeIf { it.isNotBlank() } ?: return null
    val clockInAt = parseOptionalInstant(clockIn) ?: return null
    val clockOutAt = parseOptionalInstant(clockOut) ?: return null
    val reviewedAtInstant = parseOptionalInstant(justificationReviewedAt) ?: return null
    return TeamAttendance(
        id = id,
        membershipId = member,
        branchId = branch,
        shiftId = shiftId,
        clockIn = clockInAt.value,
        clockOut = clockOutAt.value,
        status = status.toEnumOrNull<AttendanceStatus>(),
        distanceToBranchMeters = distanceToBranchMeters,
        workedMinutes = workedMinutes,
        overtimeMinutes = overtimeMinutes,
        justificationReason = justificationReason,
        justificationStatus = justificationStatus.toEnumOrNull<JustificationStatus>(),
        reviewedBy = justificationReviewedBy,
        reviewedAt = reviewedAtInstant.value
    )
}

private class Parsed(val value: Instant?)

/** Null si el texto existe pero no es una fecha válida; `Parsed(null)` si no hay texto. */
private fun parseOptionalInstant(text: String?): Parsed? {
    if (text.isNullOrBlank()) return Parsed(null)
    return try {
        Parsed(Instant.parse(text))
    } catch (_: DateTimeParseException) {
        null
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
