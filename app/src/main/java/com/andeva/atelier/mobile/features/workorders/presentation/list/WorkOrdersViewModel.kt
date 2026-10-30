package com.andeva.atelier.mobile.features.workorders.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lista de órdenes de la sede (`operations:work_orders:read`). Observa la caché Room y la recarga al abrir; si la
 * recarga falla se sigue con la caché y, al volver la red, se actualiza sola.
 */
@HiltViewModel
class WorkOrdersViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshOrders: RefreshBranchWorkOrdersUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkOrdersUiState())
    val uiState: StateFlow<WorkOrdersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canRead = session?.has(Permission.WORK_ORDERS_READ) == true) }
            }
        }
        viewModelScope.launch {
            observeOrders().collect { orders -> _uiState.update { it.copy(orders = orders, isLoaded = true) } }
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
}
