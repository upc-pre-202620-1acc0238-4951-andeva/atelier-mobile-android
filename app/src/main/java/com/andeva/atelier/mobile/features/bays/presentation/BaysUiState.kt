package com.andeva.atelier.mobile.features.bays.presentation

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus

data class BaysUiState(
    val board: List<BayBoardItem> = emptyList(),
    val activeOrders: List<WorkOrder> = emptyList(),
    val statusFilter: BayStatus? = null,
    val isRefreshing: Boolean = false,
    val isAssigning: Boolean = false,
    val targetBayForAssign: WorkBay? = null,
    val canReadBays: Boolean = false,
    val canAssignBay: Boolean = false,
    val canManageBays: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isCreatingBay: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val filteredBoard: List<BayBoardItem>
        get() = if (statusFilter == null) board else board.filter { it.bay.status == statusFilter }

    val availableCount: Int get() = board.count { it.bay.status == BayStatus.AVAILABLE }
    val occupiedCount: Int get() = board.count { it.bay.status == BayStatus.OCCUPIED }
    val maintenanceCount: Int get() = board.count { it.bay.status == BayStatus.MAINTENANCE }

    val assignableOrders: List<WorkOrder>
        get() = activeOrders.filter {
            it.status == WorkOrderStatus.IN_PROGRESS || it.status == WorkOrderStatus.DRAFT
        }
}
