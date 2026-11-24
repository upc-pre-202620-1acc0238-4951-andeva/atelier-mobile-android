package com.andeva.atelier.mobile.features.inventory.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.GetStockAlertsUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObserveStockAlertsUseCase
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class StockAlertsUiState(
    val alerts: List<StockAlert> = emptyList(),
    val isRefreshing: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = true,
    val canCreatePurchaseOrder: Boolean = false
) {
    val outOfStockCount: Int get() = alerts.count { it.currentStock <= BigDecimal.ZERO }
}

/**
 * US34 E1: bandeja de alertas de reposición. Room pinta primero (alertas derivadas de la caché `parts`) y
 * `GET /inventory/alerts` las reemplaza. `resolve` no se usa: no cambia nada en el backend (H13); en su lugar
 * se ofrece ir a órdenes de compra (`PURCHASE_ORDERS_CREATE`).
 */
@HiltViewModel
class StockAlertsViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeAlerts: ObserveStockAlertsUseCase,
    private val getAlerts: GetStockAlertsUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAlertsUiState())
    val uiState: StateFlow<StockAlertsUiState> = _uiState.asStateFlow()

    /** Con la respuesta del backend en pantalla, la caché ya no la pisa. */
    private var remoteLoaded = false

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canRead = session?.has(Permission.ALERTS_READ) == true,
                        canCreatePurchaseOrder = session?.has(Permission.PURCHASE_ORDERS_CREATE) == true
                    )
                }
            }
        }
        viewModelScope.launch {
            observeAlerts().collect { alerts -> if (!remoteLoaded) _uiState.update { it.copy(alerts = sorted(alerts)) } }
        }
        viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } } }
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            getAlerts()
                .onSuccess { alerts ->
                    remoteLoaded = true
                    _uiState.update { it.copy(alerts = sorted(alerts), isRefreshing = false) }
                }
                .onFailure { e -> _uiState.update { it.copy(isRefreshing = false, error = e.toUiText()) } }
        }
    }

    /** Agotados primero y, después, los que más faltan. */
    private fun sorted(alerts: List<StockAlert>) =
        alerts.sortedWith(compareBy<StockAlert> { it.currentStock > BigDecimal.ZERO }.thenByDescending { it.deficit })
}
