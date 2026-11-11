package com.andeva.atelier.mobile.features.main.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.notifications.NotificationRecord
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.attendance.application.GetBranchDailyAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.inventory.application.GetStockAlertsUseCase
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val pushInbox: PushInbox,
    private val getStockAlerts: GetStockAlertsUseCase,
    private val getBranchDailyAttendance: GetBranchDailyAttendanceUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    observePendingSyncCount: ObservePendingOperationCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pushInbox.history.collect { items ->
                _uiState.update { it.copy(pushHistory = items) }
            }
        }
        viewModelScope.launch {
            observeOrders().collect { orders ->
                _uiState.update {
                    it.copy(draftOrdersWaitingApproval = orders.filter { o -> o.status == WorkOrderStatus.DRAFT })
                }
            }
        }
        viewModelScope.launch {
            observePendingSyncCount().collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val session = observeSession().firstOrNull()
            val stockAlerts = if (session?.has(Permission.INVENTORY_ALERTS_READ) == true ||
                session?.has(Permission.PARTS_READ) == true
            ) {
                getStockAlerts().getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val justifications = if (session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true) {
                getBranchDailyAttendance().getOrDefault(emptyList()).filter {
                    it.attendance.justificationStatus == JustificationStatus.PENDING
                }
            } else {
                emptyList()
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    stockAlerts = stockAlerts,
                    pendingJustifications = justifications
                )
            }
        }
    }

    fun onOpenPushRecord(record: NotificationRecord) {
        pushInbox.markRead(record.id)
        record.destination?.let(pushInbox::post)
    }

    fun onMarkAllRead() {
        pushInbox.markAllRead()
    }
}
