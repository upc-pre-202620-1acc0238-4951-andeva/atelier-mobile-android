package com.andeva.atelier.mobile.features.attendance.presentation.dashboard

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule
import com.andeva.atelier.mobile.features.cashier.domain.DailyCashSummary
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.time.LocalDate

/**
 * Estado único del Hub de Piso y Mostrador (`FloorHubScreen`). Compone únicamente las secciones
 * que los permisos atómicos de la sesión habilitan (Técnico, Recepcionista, Almacén, Cajero o híbridos).
 */
data class FloorHubUiState(
    val todayDate: LocalDate = LocalDate.MIN,
    val session: Session? = null,
    val schedule: WeeklySchedule? = null,
    val todayAttendance: TodayAttendance = TodayAttendance.NotClockedIn,
    val deviceLocation: DeviceLocation? = null,
    val assignedTasks: List<Task> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val branchOrders: List<WorkOrder> = emptyList(),
    val stockAlerts: List<StockAlert> = emptyList(),
    val issuedPurchaseOrders: List<PurchaseOrder> = emptyList(),
    val catalogSize: Int = 0,
    val dailyCash: DailyCashSummary? = null,
    val activeObdCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val isOnline: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val isSubmittingAttendance: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val fullName: String get() = session?.fullName.orEmpty()
    val tenantName: String get() = session?.tenantName.orEmpty()
    val branchName: String? get() = schedule?.branch?.name

    val canClockInPerm: Boolean get() = session?.has(Permission.ATTENDANCE_CLOCK_IN) == true
    val canClockOutPerm: Boolean get() = session?.has(Permission.ATTENDANCE_CLOCK_OUT) == true
    val canTrackTasks: Boolean get() = session?.has(Permission.TASKS_TRACK_TIME) == true &&
        session.has(Permission.TASKS_READ)
    val canUseCounter: Boolean get() = session?.has(Permission.APPOINTMENTS_READ) == true ||
        session?.has(Permission.CUSTOMERS_CREATE) == true
    val canManageStock: Boolean get() = session?.has(Permission.PARTS_MANAGE) == true ||
        session?.has(Permission.PURCHASE_ORDERS_CREATE) == true ||
        session?.has(Permission.BATCHES_RECEIVE) == true
    val canOperateCash: Boolean get() = session?.has(Permission.INVOICES_ISSUE_SUNAT) == true ||
        session?.has(Permission.PAYMENTS_CREATE) == true

    val canReportFinding: Boolean get() = session?.has(Permission.PROPOSALS_CREATE) == true
    val canUseObd: Boolean get() = session?.canUseTelemetry() == true ||
        session?.canReadTelemetry() == true ||
        session?.has(Permission.DEVICES_READ) == true
    val canReadParts: Boolean get() = session?.has(Permission.PARTS_READ) == true
    val canReadBays: Boolean get() = session?.has(Permission.BAYS_READ) == true

    val activeTask: Task?
        get() = assignedTasks.firstOrNull { it.status == TaskStatus.IN_PROGRESS }
            ?: assignedTasks.firstOrNull { it.status == TaskStatus.ON_HOLD }
    val pendingTasksCount: Int get() = assignedTasks.count { it.status != TaskStatus.COMPLETED }
    val completedTasksCount: Int get() = assignedTasks.count { it.status == TaskStatus.COMPLETED }
    val inProgressTasksCount: Int get() = assignedTasks.count { it.status == TaskStatus.IN_PROGRESS }

    val readyToBillOrders: List<WorkOrder>
        get() = branchOrders.filter { it.status == WorkOrderStatus.COMPLETED }
    val outOfStockCount: Int
        get() = stockAlerts.count { it.currentStock.signum() <= 0 }

    val canClockInNow: Boolean
        get() = canClockInPerm && !isSubmittingAttendance &&
            session?.branchId != null && session.shiftId != null &&
            todayAttendance is TodayAttendance.NotClockedIn

    val canClockOutNow: Boolean
        get() = canClockOutPerm && !isSubmittingAttendance &&
            (todayAttendance is TodayAttendance.ClockedIn || todayAttendance is TodayAttendance.ClockInQueued)

    val showSkeleton: Boolean get() = !hasLoaded && assignedTasks.isEmpty() && appointments.isEmpty() &&
        stockAlerts.isEmpty() && branchOrders.isEmpty()
}
