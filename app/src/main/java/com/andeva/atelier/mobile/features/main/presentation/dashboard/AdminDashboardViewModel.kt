package com.andeva.atelier.mobile.features.main.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.attendance.application.GetBranchDailyAttendanceUseCase
import com.andeva.atelier.mobile.features.auth.application.GetSelectableBranchesUseCase
import com.andeva.atelier.mobile.features.bays.application.ObserveBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.RefreshBayBoardUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetObdDevicesUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetStockAlertsUseCase
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveWorkshopVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshCustomersUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.EnrichReceptionReferencesUseCase
import com.andeva.atelier.mobile.features.team.application.ApproveJustificationUseCase
import com.andeva.atelier.mobile.features.team.application.GetPendingJustificationsUseCase
import com.andeva.atelier.mobile.features.team.application.RejectJustificationUseCase
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Cuánto se queda a la vista el resultado de una decisión antes de retirarse de la lista. */
private const val RESOLVED_LINGER_MS = 1_600L

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getBranches: GetSelectableBranchesUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshOrders: RefreshBranchWorkOrdersUseCase,
    observeBayBoard: ObserveBayBoardUseCase,
    private val refreshBayBoard: RefreshBayBoardUseCase,
    private val enrichReferences: EnrichReceptionReferencesUseCase,
    observeAppointments: ObserveAppointmentsUseCase,
    private val refreshAppointments: RefreshAppointmentsUseCase,
    observeCustomers: ObserveCustomersUseCase,
    observeVehicles: ObserveWorkshopVehiclesUseCase,
    private val refreshCustomers: RefreshCustomersUseCase,
    private val getStockAlerts: GetStockAlertsUseCase,
    private val getObdDevices: GetObdDevicesUseCase,
    private val getBranchDailyAttendance: GetBranchDailyAttendanceUseCase,
    private val getPendingJustifications: GetPendingJustificationsUseCase,
    private val approveJustification: ApproveJustificationUseCase,
    private val rejectJustification: RejectJustificationUseCase,
    observePendingSyncCount: ObservePendingOperationCountUseCase,
    connectivity: ConnectivityObserver,
    pushInbox: PushInbox,
    clock: Clock
) : ViewModel() {

    private val today = BusinessTime.limaDate(clock.instant())

    private val _uiState = MutableStateFlow(AdminDashboardUiState(today = today))
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private var lastBranchId: String? = null
    private val enrichedIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val branchChanged = lastBranchId != null && session?.branchId != lastBranchId
                lastBranchId = session?.branchId
                _uiState.update { state ->
                    state.copy(
                        fullName = session?.fullName.orEmpty(),
                        tenantName = session?.tenantName.orEmpty(),
                        branchId = session?.branchId,
                        canCreateWorkOrder = session?.has(Permission.WORK_ORDERS_CREATE) == true,
                        canReadWorkOrders = session?.has(Permission.WORK_ORDERS_READ) == true,
                        canStartWorkOrders = session?.has(Permission.WORK_ORDERS_UPDATE) == true,
                        canReadBays = session?.has(Permission.BAYS_READ) == true,
                        canReadAppointments = session?.has(Permission.APPOINTMENTS_READ) == true,
                        canReadCustomers = session?.has(Permission.CUSTOMERS_READ) == true,
                        canRegisterCustomers = session?.has(Permission.CUSTOMERS_CREATE) == true,
                        canReadInventory = session?.has(Permission.INVENTORY_ALERTS_READ) == true ||
                            session?.has(Permission.PARTS_READ) == true,
                        canReadObd = session?.has(Permission.OBD_DEVICES_READ) == true &&
                            session.plan?.iotTelemetryEnabled != false,
                        canAuditAttendance = session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true,
                        canApproveJustifications = session?.has(Permission.JUSTIFICATIONS_APPROVE) == true
                    )
                }
                if (branchChanged) refresh()
            }
        }
        viewModelScope.launch {
            observeOrders().collect { orders ->
                _uiState.update { it.copy(orders = orders) }
                val missingVehicles = orders.filter { it.plate == null }.map { it.vehicleId }.filter { it !in enrichedIds }.toSet()
                val missingCustomers = orders.filter { it.customerName == null }.map { it.customerId }.filter { it !in enrichedIds }.toSet()
                if (missingVehicles.isNotEmpty() || missingCustomers.isNotEmpty()) {
                    enrichedIds += missingVehicles
                    enrichedIds += missingCustomers
                    enrichReferences(missingVehicles, missingCustomers)
                }
            }
        }
        viewModelScope.launch { observeBayBoard().collect { bays -> _uiState.update { it.copy(bays = bays) } } }
        viewModelScope.launch {
            observeAppointments(today).collect { list -> _uiState.update { it.copy(appointments = list) } }
        }
        viewModelScope.launch {
            observeCustomers().collect { list -> _uiState.update { it.copy(customersCount = list.size) } }
        }
        viewModelScope.launch {
            observeVehicles().collect { list -> _uiState.update { it.copy(vehiclesCount = list.size) } }
        }
        viewModelScope.launch {
            observePendingSyncCount().collect { count -> _uiState.update { it.copy(pendingSyncCount = count) } }
        }
        viewModelScope.launch {
            pushInbox.history.collect { records ->
                _uiState.update { it.copy(unreadNotificationsCount = records.count { r -> !r.read }) }
            }
        }
        viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } } }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            val session = observeSession().firstOrNull()
            var failure: Throwable? = null
            if (session?.has(Permission.BRANCHES_READ) == true) {
                getBranches().onSuccess { branches ->
                    _uiState.update { state ->
                        state.copy(
                            branchName = branches.firstOrNull { it.id == state.branchId }?.name,
                            canChangeBranch = branches.size > 1
                        )
                    }
                }
            }
            if (session?.has(Permission.WORK_ORDERS_READ) == true) {
                refreshOrders().onFailure { failure = it }
            }
            if (session?.has(Permission.BAYS_READ) == true) {
                refreshBayBoard()
            }
            if (session?.has(Permission.APPOINTMENTS_READ) == true) {
                refreshAppointments(today)
            }
            if (session?.has(Permission.CUSTOMERS_READ) == true) {
                refreshCustomers(force = false)
            }
            if (session?.has(Permission.INVENTORY_ALERTS_READ) == true || session?.has(Permission.PARTS_READ) == true) {
                getStockAlerts().onSuccess { alerts -> _uiState.update { it.copy(stockAlerts = alerts) } }
            }
            if (session?.has(Permission.OBD_DEVICES_READ) == true && session.plan?.iotTelemetryEnabled != false) {
                getObdDevices(status = null).onSuccess { devices -> _uiState.update { it.copy(obdDevices = devices) } }
            }
            if (session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true) {
                getBranchDailyAttendance().onSuccess { records ->
                    _uiState.update { it.copy(branchAttendances = records) }
                }
                getPendingJustifications().onSuccess { pending ->
                    _uiState.update {
                        it.copy(
                            justifications = pending.items,
                            failedJustificationDays = pending.failedDates.size,
                            resolved = emptyMap()
                        )
                    }
                }
            }
            _uiState.update { it.copy(isRefreshing = false, hasLoaded = true, error = failure?.toUiText()) }
        }
    }

    fun onOpenJustification(item: TeamAttendance) {
        if (item.id in _uiState.value.resolved) return
        _uiState.update { it.copy(reviewing = item, reviewError = null) }
    }

    fun onDismissJustification() {
        if (_uiState.value.isSubmittingReview) return
        _uiState.update { it.copy(reviewing = null, reviewError = null) }
    }

    fun onReviewJustification(approve: Boolean) {
        val state = _uiState.value
        val item = state.reviewing ?: return
        if (!state.canApproveJustifications || state.isSubmittingReview) return
        _uiState.update { it.copy(isSubmittingReview = true, reviewError = null) }
        viewModelScope.launch {
            val result = if (approve) approveJustification(item.id) else rejectJustification(item.id)
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isSubmittingReview = false, reviewing = null, resolved = it.resolved + (item.id to approve))
                    }
                    delay(RESOLVED_LINGER_MS)
                    _uiState.update { s ->
                        s.copy(justifications = s.justifications.filterNot { it.id == item.id }, resolved = s.resolved - item.id)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isSubmittingReview = false, reviewError = e.toUiText()) } }
        }
    }
}
