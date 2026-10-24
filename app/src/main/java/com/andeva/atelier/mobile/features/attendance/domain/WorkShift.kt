package com.andeva.atelier.mobile.features.attendance.domain

import java.time.DayOfWeek
import java.time.LocalTime

/** Turno (`WorkShiftResource` de docs/api/hr.md). Las horas son de Lima. */
data class WorkShift(
    val id: String,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val gracePeriodMinutes: Int,
    val workDays: Set<DayOfWeek>,
    val restDays: Set<DayOfWeek>,
    val isActive: Boolean
)
