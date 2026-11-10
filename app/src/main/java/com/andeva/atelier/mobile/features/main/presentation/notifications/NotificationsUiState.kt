package com.andeva.atelier.mobile.features.main.presentation.notifications

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.notifications.NotificationRecord
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder

data class NotificationsUiState(
    val pushHistory: List<NotificationRecord> = emptyList(),
    val stockAlerts: List<StockAlert> = emptyList(),
    val pendingJustifications: List<BranchAttendanceItem> = emptyList(),
    val draftOrdersWaitingApproval: List<WorkOrder> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isLoading: Boolean = true,
    val error: UiText? = null
) {
    val unreadPushCount: Int get() = pushHistory.count { !it.read }
    val totalActiveAlerts: Int
        get() = stockAlerts.size + pendingJustifications.size + draftOrdersWaitingApproval.size +
            (if (pendingSyncCount > 0) 1 else 0)
}
