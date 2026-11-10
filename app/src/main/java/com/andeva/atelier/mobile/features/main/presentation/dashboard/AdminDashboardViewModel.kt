package com.andeva.atelier.mobile.features.main.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.EnrichReceptionReferencesUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveWorkshopVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshCustomersUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getBranches: GetSelectableBranchesUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshOrders: RefreshBranchWorkOrdersUseCase,
    observeBayBoard: ObserveBayBoardUseCase,
    private val refreshBayBoard: RefreshBayBoardUseCase,
    observeAppointments: ObserveAppointmentsUseCase,
    private val refreshAppointments: RefreshAppointmentsUseCase,
    observeCustomers: ObserveCustomersUseCase,
    observeVehicles: ObserveWorkshopVehiclesUseCase,
    private val refreshCustomers: RefreshCustomersUseCase,
    private val enrichReferences: EnrichReceptionReferencesUseCase,
    private val getStockAlerts: GetStockAlertsUseCase,
    private val getBranchDailyAttendance: GetBranchDailyAttendanceUseCase,
    private val getObdDevices: GetObdDevicesUseCase,
    observePendingSyncCount: ObservePendingOperationCountUseCase,
    pushInbox: PushInbox,
    clock: Clock
) : ViewModel() {

    private val today = BusinessTime.limaDate(clock.instant())

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private var lastBranchId: String? = null
    private val enrichedIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val branchChanged = lastBranchId != null && session?.branchId != lastBranchId
                lastBranchId = session?.branchId
                _uiState.update { state ->
                    val matchedBranchName = state.branches.firstOrNull { it.id == session?.branchId }?.name
                    state.copy(
                        fullName = session?.fullName.orEmpty(),
                        tenantName = session?.tenantName.orEmpty(),
                        branchId = session?.branchId,
                        branchName = matchedBranchName ?: state.branchName,
                        canCreateWorkOrder = session?.has(Permission.WORK_ORDERS_CREATE) == true,
                        canReadWorkOrders = session?.has(Permission.WORK_ORDERS_READ) == true,
                        canReadBays = session?.has(Permission.BAYS_READ) == true,
                        canReadAppointments = session?.has(Permission.APPOINTMENTS_READ) == true,
                        canReadInventory = session?.has(Permission.INVENTORY_ALERTS_READ) == true ||
                            session?.has(Permission.PARTS_READ) == true,
                        canAuditAttendance = session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true,
                        canReadObd = session?.has(Permission.OBD_DEVICES_READ) == true
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
        viewModelScope.launch {
            observeBayBoard().collect { bays ->
                _uiState.update { it.copy(bays = bays) }
            }
        }
        viewModelScope.launch {
            observeAppointments(today).collect { appointments ->
                _uiState.update { it.copy(appointments = appointments) }
            }
        }
        viewModelScope.launch {
            observeCustomers().collect { customers ->
                _uiState.update { it.copy(customersCount = customers.size) }
            }
        }
        viewModelScope.launch {
            observeVehicles().collect { vehicles ->
                _uiState.update { it.copy(vehiclesCount = vehicles.size) }
            }
        }
        viewModelScope.launch {
            observePendingSyncCount().collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }
        viewModelScope.launch {
            pushInbox.history.collect { records ->
                _uiState.update { it.copy(unreadNotificationsCount = records.count { r -> !r.read }) }
            }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            val session = observeSession().firstOrNull()
            if (session?.has(Permission.BRANCHES_READ) == true) {
                getBranches().onSuccess { branches ->
                    _uiState.update { state ->
                        state.copy(
                            branches = branches,
                            branchName = branches.firstOrNull { it.id == state.branchId }?.name
                        )
                    }
                }
            }
            if (session?.has(Permission.WORK_ORDERS_READ) == true) {
                refreshOrders()
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
                getStockAlerts().onSuccess { alerts ->
                    _uiState.update { it.copy(stockAlerts = alerts) }
                }
            }
            if (session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true) {
                getBranchDailyAttendance().onSuccess { records ->
                    _uiState.update { it.copy(branchAttendances = records) }
                }
            }
            if (session?.has(Permission.OBD_DEVICES_READ) == true) {
                getObdDevices(status = null).onSuccess { devices ->
                    _uiState.update { it.copy(obdDevices = devices) }
                }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
