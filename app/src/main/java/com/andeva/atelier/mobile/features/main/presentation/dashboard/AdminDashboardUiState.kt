package com.andeva.atelier.mobile.features.main.presentation.dashboard

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.auth.domain.BranchOption
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus

data class AdminDashboardUiState(
    val fullName: String = "",
    val tenantName: String = "",
    val branchId: String? = null,
    val branchName: String? = null,
    val branches: List<BranchOption> = emptyList(),
    val orders: List<WorkOrder> = emptyList(),
    val bays: List<BayBoardItem> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val stockAlerts: List<StockAlert> = emptyList(),
    val branchAttendances: List<BranchAttendanceItem> = emptyList(),
    val obdDevices: List<ObdDevice> = emptyList(),
    val customersCount: Int = 0,
    val vehiclesCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    val canCreateWorkOrder: Boolean = false,
    val canReadWorkOrders: Boolean = false,
    val canReadBays: Boolean = false,
    val canReadAppointments: Boolean = false,
    val canReadInventory: Boolean = false,
    val canAuditAttendance: Boolean = false,
    val canReadObd: Boolean = false
) {
    val draftOrders: List<WorkOrder> get() = orders.filter { it.status == WorkOrderStatus.DRAFT }
    val inProgressOrders: List<WorkOrder> get() = orders.filter { it.status == WorkOrderStatus.IN_PROGRESS }
    val readyOrders: List<WorkOrder> get() = orders.filter { it.status == WorkOrderStatus.COMPLETED }

    val availableBaysCount: Int get() = bays.count { it.bay.status == BayStatus.AVAILABLE }
    val occupiedBaysCount: Int get() = bays.count { it.bay.status == BayStatus.OCCUPIED }

    val pendingJustificationsCount: Int
        get() = branchAttendances.count { it.attendance.justificationStatus == JustificationStatus.PENDING }

    val activeObdCount: Int get() = obdDevices.count { it.status == DeviceStatus.ACTIVE }

    val totalBadgeAlerts: Int
        get() = unreadNotificationsCount + stockAlerts.size + pendingJustificationsCount
}
