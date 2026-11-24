package com.andeva.atelier.mobile.features.main.presentation.dashboard

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.workorders.domain.PendingSuspensionItem
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.time.LocalDate

/**
 * "Hoy": el panel de la sede. Todo sale de endpoints de lista (órdenes abiertas, tablero de bahías, citas, clientes,
 * alertas de stock, asistencia del día, escáneres OBD y descargos por revisar). Las propuestas y suspensiones no tienen bandeja (H5): se resuelven en la orden.
 */
data class AdminDashboardUiState(
    val today: LocalDate = LocalDate.MIN,
    val fullName: String = "",
    val tenantName: String = "",
    val branchId: String? = null,
    val branchName: String? = null,
    val canChangeBranch: Boolean = false,
    val orders: List<WorkOrder> = emptyList(),
    val bays: List<BayBoardItem> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val stockAlerts: List<StockAlert> = emptyList(),
    val obdDevices: List<ObdDevice> = emptyList(),
    val customersCount: Int = 0,
    val vehiclesCount: Int = 0,
    val branchAttendances: List<BranchAttendanceItem> = emptyList(),
    val justifications: List<TeamAttendance> = emptyList(),
    val pendingSuspensions: List<PendingSuspensionItem> = emptyList(),
    /** Descargos ya resueltos (id → aprobado) que aún muestran su resultado antes de retirarse de la lista. */
    val resolved: Map<String, Boolean> = emptyMap(),
    /** Días de descargos cuya consulta falló (H32): el resultado es parcial y se avisa. */
    val failedJustificationDays: Int = 0,
    val reviewing: TeamAttendance? = null,
    val isSubmittingReview: Boolean = false,
    val reviewError: UiText? = null,
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canCreateWorkOrder: Boolean = false,
    val canReadWorkOrders: Boolean = false,
    val canStartWorkOrders: Boolean = false,
    val canReviewSuspensions: Boolean = false,
    val canReadBays: Boolean = false,
    val canReadAppointments: Boolean = false,
    val canReadCustomers: Boolean = false,
    val canRegisterCustomers: Boolean = false,
    val canReadInventory: Boolean = false,
    val canReadObd: Boolean = false,
    val canAuditAttendance: Boolean = false,
    val canApproveJustifications: Boolean = false,
    val canClockPersonalAttendance: Boolean = false,
    val personalTodayAttendance: TodayAttendance = TodayAttendance.NotClockedIn,
    val canWorkOwnTasks: Boolean = false,
    val activePersonalTasksCount: Int = 0
) {
    /** La caché de la sede no guarda órdenes cerradas: toda orden aquí es un auto en el taller. */
    val carsInWorkshop: Int get() = orders.size
    val readyOrdersCount: Int get() = orders.count { it.status == WorkOrderStatus.COMPLETED }
    val occupiedBaysCount: Int get() = bays.count { it.bay.status == BayStatus.OCCUPIED }
    val onShiftCount: Int get() = branchAttendances.count { it.attendance.isOpen }
    val availableBaysCount: Int get() = bays.count { it.bay.status == BayStatus.AVAILABLE }
    val inProgressCount: Int get() = orders.count { it.status == WorkOrderStatus.IN_PROGRESS }
    val draftOrdersCount: Int get() = orders.count { it.status == WorkOrderStatus.DRAFT }
    val pendingJustificationsCount: Int get() = justifications.size
    val activeObdCount: Int get() = obdDevices.count { it.status == DeviceStatus.ACTIVE }

    /** Órdenes abiertas sin iniciar: alguien con `work_orders:update` debe revisar la proforma y darles inicio. */
    val draftOrders: List<WorkOrder>
        get() = if (canStartWorkOrders) orders.filter { it.status == WorkOrderStatus.DRAFT } else emptyList()

    val visiblePendingSuspensions: List<PendingSuspensionItem>
        get() = if (canReviewSuspensions) pendingSuspensions else emptyList()

    val decisionsCount: Int
        get() = draftOrders.size + visiblePendingSuspensions.size + justifications.count { it.id !in resolved }

    /** Esqueleto solo sin caché: primera carga y nada que pintar todavía. */
    val showSkeleton: Boolean get() = !hasLoaded && orders.isEmpty()
}
