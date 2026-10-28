package com.andeva.atelier.mobile.features.attendance.presentation

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule

/** Estado único de la pantalla de asistencia; la fuente de verdad es Room/cola/backend. */
data class AttendanceUiState(
    val session: Session? = null,
    val today: TodayAttendance = TodayAttendance.NotClockedIn,
    val schedule: WeeklySchedule? = null,
    val history: List<Attendance> = emptyList(),
    val isRefreshing: Boolean = true,
    val isSubmitting: Boolean = false,
    val justificationFor: Attendance? = null,
    val justification: String = "",
    val error: UiText? = null,
    val notice: UiText? = null,
    val locationWarning: UiText? = null,
    val deviceLocation: DeviceLocation? = null,
    val isLocating: Boolean = false
) {
    val canClockIn: Boolean
        get() = !isSubmitting && session?.branchId != null && session.shiftId != null && today is TodayAttendance.NotClockedIn

    val canClockOut: Boolean
        get() = !isSubmitting && (today is TodayAttendance.ClockedIn || today is TodayAttendance.ClockInQueued)
}
