package com.andeva.atelier.mobile.features.attendance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.attendance.application.ClockInUseCase
import com.andeva.atelier.mobile.features.attendance.application.ClockOutUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveTodayAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveWeeklyScheduleUseCase
import com.andeva.atelier.mobile.features.attendance.application.RefreshAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import com.andeva.atelier.mobile.features.attendance.presentation.toAttendanceUiText
import com.andeva.atelier.mobile.features.cashier.application.GetDailyCashSummaryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetObdDevicesUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrdersUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetStockAlertsUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObserveStockAlertsUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.application.RefreshMyTasksUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class FloorHubViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    observeToday: ObserveTodayAttendanceUseCase,
    observeSchedule: ObserveWeeklyScheduleUseCase,
    private val refreshAttendance: RefreshAttendanceUseCase,
    private val clockInUseCase: ClockInUseCase,
    private val clockOutUseCase: ClockOutUseCase,
    private val locationProvider: LocationProvider,
    observeMyTasks: ObserveMyTasksUseCase,
    private val refreshMyTasks: RefreshMyTasksUseCase,
    observeAppointments: ObserveAppointmentsUseCase,
    private val refreshAppointments: RefreshAppointmentsUseCase,
    observeBranchOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshBranchOrders: RefreshBranchWorkOrdersUseCase,
    observePartsCatalog: ObservePartsCatalogUseCase,
    private val refreshPartsCatalog: RefreshPartsCatalogUseCase,
    observeStockAlerts: ObserveStockAlertsUseCase,
    private val getStockAlerts: GetStockAlertsUseCase,
    private val getPurchaseOrders: GetPurchaseOrdersUseCase,
    private val getDailyCashSummary: GetDailyCashSummaryUseCase,
    private val getObdDevices: GetObdDevicesUseCase,
    observePendingCount: ObservePendingOperationCountUseCase,
    connectivity: ConnectivityObserver,
    pushInbox: PushInbox,
    clock: Clock
) : ViewModel() {

    private val today = BusinessTime.limaDate(clock.instant())

    private val _uiState = MutableStateFlow(FloorHubUiState(todayDate = today))
    val uiState: StateFlow<FloorHubUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sessionManager.session, observeToday(), observeSchedule()) { session, todayAtt, schedule ->
                Triple(session, todayAtt, schedule)
            }.collect { (session, todayAtt, schedule) ->
                _uiState.update {
                    it.copy(session = session, todayAttendance = todayAtt, schedule = schedule)
                }
            }
        }
        viewModelScope.launch {
            observeMyTasks().collect { tasks -> _uiState.update { it.copy(assignedTasks = tasks) } }
        }
        viewModelScope.launch {
            observeAppointments(today).collect { list -> _uiState.update { it.copy(appointments = list) } }
        }
        viewModelScope.launch {
            observeBranchOrders().collect { orders -> _uiState.update { it.copy(branchOrders = orders) } }
        }
        viewModelScope.launch {
            observePartsCatalog(null).collect { parts -> _uiState.update { it.copy(catalogSize = parts.size) } }
        }
        viewModelScope.launch {
            observeStockAlerts().collect { cachedAlerts ->
                _uiState.update { state ->
                    if (state.stockAlerts.isEmpty()) state.copy(stockAlerts = cachedAlerts) else state
                }
            }
        }
        viewModelScope.launch {
            observePendingCount().collect { count -> _uiState.update { it.copy(pendingSyncCount = count) } }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } }
        }
        viewModelScope.launch {
            pushInbox.history.collect { records ->
                _uiState.update { it.copy(unreadNotificationsCount = records.count { r -> !r.read }) }
            }
        }
        refresh()
    }

    fun previewLocation() {
        viewModelScope.launch {
            locationProvider.currentLocation().onSuccess { location ->
                _uiState.update { it.copy(deviceLocation = location) }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                error = UiText.Resource(R.string.attendance_location_permission_required),
                deviceLocation = null
            )
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null, notice = null) }
            val session = sessionManager.session.firstOrNull()
            val jobs = buildList {
                if (session?.has(Permission.ATTENDANCE_READ_OWN) == true) {
                    add(launch { refreshAttendance() })
                }
                if (session?.has(Permission.TASKS_READ) == true) {
                    add(launch { refreshMyTasks() })
                }
                if (session?.has(Permission.APPOINTMENTS_READ) == true) {
                    add(launch { refreshAppointments(today) })
                }
                if (session?.has(Permission.WORK_ORDERS_READ) == true) {
                    add(launch { refreshBranchOrders() })
                }
                if (session?.has(Permission.PARTS_READ) == true) {
                    add(launch { refreshPartsCatalog(force = false) })
                }
                if (session?.has(Permission.ALERTS_READ) == true) {
                    add(launch {
                        getStockAlerts().onSuccess { alerts ->
                            _uiState.update { it.copy(stockAlerts = alerts) }
                        }
                    })
                }
                if (session?.has(Permission.PURCHASE_ORDERS_READ) == true) {
                    add(launch {
                        getPurchaseOrders(PurchaseOrderStatus.ISSUED).onSuccess { pos ->
                            _uiState.update { it.copy(issuedPurchaseOrders = pos) }
                        }
                    })
                }
                if (session?.has(Permission.INVOICES_READ) == true) {
                    add(launch {
                        getDailyCashSummary(today).onSuccess { summary ->
                            _uiState.update { it.copy(dailyCash = summary) }
                        }
                    })
                }
                if (session?.has(Permission.DEVICES_READ) == true && session.plan?.iotTelemetryEnabled != false) {
                    add(launch {
                        getObdDevices(DeviceStatus.ACTIVE).onSuccess { list ->
                            _uiState.update { it.copy(activeObdCount = list.size) }
                        }
                    })
                }
            }
            jobs.forEach { it.join() }
            _uiState.update { it.copy(isRefreshing = false, hasLoaded = true) }
        }
    }

    fun clockIn() {
        val session = _uiState.value.session ?: return
        if (!session.has(Permission.ATTENDANCE_CLOCK_IN)) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.attendance_permission_clock_in)) }
            return
        }
        if (session.branchId.isNullOrBlank() || session.shiftId.isNullOrBlank()) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.attendance_missing_assignment)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAttendance = true, error = null, notice = null) }
            clockInUseCase(session.branchId, session.shiftId)
                .onSuccess { result ->
                    val msg = when (result.outcome) {
                        is SubmitOutcome.Recorded -> R.string.attendance_clock_in_recorded
                        SubmitOutcome.Queued, SubmitOutcome.AlreadyQueued -> R.string.attendance_clock_in_queued
                    }
                    _uiState.update { it.copy(isSubmittingAttendance = false, notice = UiText.Resource(msg)) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSubmittingAttendance = false, error = err.toAttendanceUiText()) }
                }
        }
    }

    fun clockOut() {
        val session = _uiState.value.session ?: return
        if (!session.has(Permission.ATTENDANCE_CLOCK_OUT)) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.attendance_permission_clock_out)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAttendance = true, error = null, notice = null) }
            clockOutUseCase()
                .onSuccess { outcome ->
                    val msg = when (outcome) {
                        is SubmitOutcome.Recorded -> R.string.attendance_clock_out_recorded
                        SubmitOutcome.Queued, SubmitOutcome.AlreadyQueued -> R.string.attendance_clock_out_queued
                    }
                    _uiState.update { it.copy(isSubmittingAttendance = false, notice = UiText.Resource(msg)) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSubmittingAttendance = false, error = err.toAttendanceUiText()) }
                }
        }
    }
}
