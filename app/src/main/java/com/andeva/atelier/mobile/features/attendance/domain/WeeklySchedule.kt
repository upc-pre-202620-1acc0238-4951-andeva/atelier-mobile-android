package com.andeva.atelier.mobile.features.attendance.domain

import java.time.DayOfWeek

/**
 * Mi turno de lunes a domingo (US09 E1). [branchId] y [shiftId] son los de la sesión, que la UI
 * pasa a `ClockInUseCase`; [branch] es null si la sede aún no está en caché.
 */
data class WeeklySchedule(
    val branchId: String?,
    val shiftId: String,
    val shift: WorkShift,
    val branch: Branch?
) {
    data class Day(val dayOfWeek: DayOfWeek, val isWorkDay: Boolean)

    /** Un día es de descanso si está en `restDays`; sin `workDays` publicados, el resto se trabaja. */
    val days: List<Day>
        get() = DayOfWeek.entries.map { day ->
            Day(day, day !in shift.restDays && (shift.workDays.isEmpty() || day in shift.workDays))
        }
}
