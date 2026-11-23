package com.andeva.atelier.mobile.features.workorders.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.EnrichReceptionReferencesUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
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
 * Lista de órdenes de la sede (`operations:work_orders:read`). Observa la caché Room y la recarga al abrir;
 * el estado y la placa se filtran en el dispositivo. Recibir un vehículo (`work_orders:create`) es del flujo de
 * recepción: aquí solo se avisa a la navegación.
 */
@HiltViewModel
class WorkOrdersViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshOrders: RefreshBranchWorkOrdersUseCase,
    connectivity: ConnectivityObserver,
    private val enrichReferences: EnrichReceptionReferencesUseCase? = null,
    private val clock: Clock? = null,
    observePendingSyncCount: ObservePendingOperationCountUseCase? = null,
    pushInbox: PushInbox? = null
) : ViewModel() {

    constructor(
        observeSession: ObserveSessionUseCase,
        observeOrders: ObserveBranchWorkOrdersUseCase,
        refreshOrders: RefreshBranchWorkOrdersUseCase,
        connectivity: ConnectivityObserver
    ) : this(observeSession, observeOrders, refreshOrders, connectivity, null, null)

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
        if (observePendingSyncCount != null) {
            viewModelScope.launch {
                observePendingSyncCount().collect { count -> _uiState.update { it.copy(pendingSyncCount = count) } }
            }
        }
        if (pushInbox != null) {
            viewModelScope.launch {
                pushInbox.history.collect { records ->
                    _uiState.update { it.copy(unreadNotificationsCount = records.count { r -> !r.read }) }
                }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
                if (online && _uiState.value.showingCache) refresh()
            }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            refreshOrders()
                .onSuccess {
                    _uiState.update {
                        it.copy(isRefreshing = false, showingCache = false, lastRefreshedAt = (clock ?: Clock.systemUTC()).instant())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRefreshing = false, showingCache = true, error = e.toWorkOrderUiText()) }
                }
        }
    }

    fun onStatusFilterChange(status: WorkOrderStatus?) = _uiState.update { it.copy(statusFilter = status) }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
}
