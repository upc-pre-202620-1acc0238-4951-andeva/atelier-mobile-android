package com.andeva.atelier.mobile.features.inventory.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrdersUseCase
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseOrdersUiState(
    val orders: List<PurchaseOrder> = emptyList(),
    /** null = todas. Por defecto las emitidas: las que se pueden recibir. */
    val status: PurchaseOrderStatus? = PurchaseOrderStatus.ISSUED,
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = true
)

/** US34 E2: órdenes de compra por estado. En línea: no hay caché local de órdenes (06 §2). */
@HiltViewModel
class PurchaseOrdersViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOrders: GetPurchaseOrdersUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseOrdersUiState())
    val uiState: StateFlow<PurchaseOrdersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canRead = session?.has(Permission.PURCHASE_ORDERS_READ) == true) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && !_uiState.value.isOnline && _uiState.value.error != null
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        refresh()
    }

    fun onStatusClick(status: PurchaseOrderStatus?) {
        if (status == _uiState.value.status) return
        _uiState.update { it.copy(status = status, orders = emptyList()) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        val status = _uiState.value.status
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            getOrders(status)
                .onSuccess { orders -> _uiState.update { it.copy(orders = orders, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUiText()) } }
        }
    }
}
