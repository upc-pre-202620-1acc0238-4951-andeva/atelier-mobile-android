package com.andeva.atelier.mobile.features.workorders.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.EnrichReceptionReferencesUseCase
import com.andeva.atelier.mobile.features.reception.application.MarkAppointmentArrivedUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Lista de órdenes de la sede (`operations:work_orders:read`). Observa la caché Room y la recarga al abrir.
 * Permite además crear una nueva orden desde citas del día o ingreso presencial.
 */
@HiltViewModel
class WorkOrdersViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshOrders: RefreshBranchWorkOrdersUseCase,
    connectivity: ConnectivityObserver,
    private val enrichReferences: EnrichReceptionReferencesUseCase? = null,
    observeAppointments: ObserveAppointmentsUseCase? = null,
    private val markAppointmentArrived: MarkAppointmentArrivedUseCase? = null,
    clock: Clock? = null
) : ViewModel() {

    constructor(
        observeSession: ObserveSessionUseCase,
        observeOrders: ObserveBranchWorkOrdersUseCase,
        refreshOrders: RefreshBranchWorkOrdersUseCase,
        connectivity: ConnectivityObserver
    ) : this(observeSession, observeOrders, refreshOrders, connectivity, null, null, null, null)

    private val today = BusinessTime.limaDate((clock ?: Clock.systemUTC()).instant())

    private val _uiState = MutableStateFlow(WorkOrdersUiState())
    val uiState: StateFlow<WorkOrdersUiState> = _uiState.asStateFlow()

    private var lastBranchId: String? = null
    private val enrichedIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val branchChanged = lastBranchId != null && session?.branchId != lastBranchId
                lastBranchId = session?.branchId
                _uiState.update {
                    it.copy(
                        canRead = session?.has(Permission.WORK_ORDERS_READ) == true,
                        canCreate = session?.has(Permission.WORK_ORDERS_CREATE) == true
                    )
                }
                if (branchChanged) refresh()
            }
        }
        viewModelScope.launch {
            observeOrders().collect { orders ->
                _uiState.update { it.copy(orders = orders, isLoaded = true) }
                val missingVehicles = orders.filter { it.plate == null }.map { it.vehicleId }.filter { it !in enrichedIds }.toSet()
                val missingCustomers = orders.filter { it.customerName == null }.map { it.customerId }.filter { it !in enrichedIds }.toSet()
                if (missingVehicles.isNotEmpty() || missingCustomers.isNotEmpty()) {
                    enrichedIds += missingVehicles
                    enrichedIds += missingCustomers
                    enrichReferences?.invoke(missingVehicles, missingCustomers)
                }
            }
        }
        if (observeAppointments != null) {
            viewModelScope.launch {
                observeAppointments(today).collect { appointments ->
                    val pendingArrival = appointments.filter {
                        it.status == AppointmentStatus.CONFIRMED || it.status == AppointmentStatus.PENDING
                    }
                    _uiState.update { it.copy(appointmentsToday = pendingArrival) }
                }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online -> if (online && _uiState.value.showingCache) refresh() }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            refreshOrders()
                .onSuccess { _uiState.update { it.copy(isRefreshing = false, showingCache = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isRefreshing = false, showingCache = true, error = e.toWorkOrderUiText()) }
                }
        }
    }

    fun onOpenNewOrderSheet() = _uiState.update {
        if (it.canCreate) it.copy(showNewOrderSheet = true, error = null) else it
    }

    fun onDismissNewOrderSheet() = _uiState.update {
        if (it.arrivingAppointmentId != null) it else it.copy(showNewOrderSheet = false)
    }

    fun onSelectAppointmentForArrival(
        appointment: Appointment,
        onOrderReady: (vehicleId: String, customerId: String, workOrderId: String) -> Unit
    ) {
        val useCase = markAppointmentArrived ?: return
        if (_uiState.value.arrivingAppointmentId != null) return
        _uiState.update { it.copy(arrivingAppointmentId = appointment.id, error = null) }
        viewModelScope.launch {
            useCase(appointment.id)
                .onSuccess { result ->
                    _uiState.update { it.copy(arrivingAppointmentId = null, showNewOrderSheet = false) }
                    when (result) {
                        is ArrivalResult.WorkOrderReady -> {
                            onOrderReady(result.appointment.vehicleId, result.appointment.customerId, result.workOrderId)
                        }
                        is ArrivalResult.WorkOrderPending -> {
                            onOrderReady(result.appointment.vehicleId, result.appointment.customerId, "")
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(arrivingAppointmentId = null, error = e.toWorkOrderUiText()) }
                }
        }
    }
}
