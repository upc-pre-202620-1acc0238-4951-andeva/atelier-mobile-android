package com.andeva.atelier.mobile.features.attendance.presentation.dashboard

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule
import com.andeva.atelier.mobile.features.tasks.domain.Task

/**
 * Estado UI del Dashboard de Empleado / Mecánico (estilo Lemon Cash para Atelier).
 */
data class EmployeeDashboardUiState(
    val session: Session? = null,
    val schedule: WeeklySchedule? = null,
    val today: TodayAttendance = TodayAttendance.NotClockedIn,
    val deviceLocation: DeviceLocation? = null,
    val activeTask: Task? = null,
    val assignedTasks: List<Task> = emptyList(),
    val pendingTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val activeObdCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isSubmittingAttendance: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val canClockIn: Boolean
        get() = today is TodayAttendance.NotClockedIn

    val canClockOut: Boolean
        get() = today is TodayAttendance.ClockedIn || today is TodayAttendance.ClockInQueued

    val hasClockedIn: Boolean
        get() = today is TodayAttendance.ClockedIn || today is TodayAttendance.ClockInQueued

    val isAttendanceQueued: Boolean
        get() = today is TodayAttendance.ClockInQueued || today is TodayAttendance.ClockOutQueued

    val fullName: String get() = session?.fullName.orEmpty()
    val tenantName: String get() = session?.tenantName.orEmpty()
    val branchName: String get() = schedule?.branch?.name ?: "Sede no asignada"

    val assignedWorkOrders: List<Task>
        get() = assignedTasks
            .filter { it.workOrderId.isNotBlank() }
            .distinctBy { it.workOrderId }
}
