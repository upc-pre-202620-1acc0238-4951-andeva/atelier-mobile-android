package com.andeva.atelier.mobile.features.attendance.infrastructure.repository

import java.time.LocalDate

/** `dedupe_key` de docs/06-offline-sync.md §3 y §6: un mismo hecho no se encola dos veces. La fecha es de Lima. */
object AttendanceDedupeKeys {
    fun clockIn(membershipId: String, date: LocalDate) = "CLOCK_IN:$membershipId:$date"

    fun clockOut(membershipId: String, date: LocalDate) = "CLOCK_OUT:$membershipId:$date"

    fun justification(attendanceId: String) = "JUSTIFICATION:$attendanceId"
}
