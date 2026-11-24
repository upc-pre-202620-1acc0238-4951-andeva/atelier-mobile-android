package com.andeva.atelier.mobile.features.inventory.presentation.valuation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.GetInventoryValuationUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Existencias de una categoría; [category] null agrupa los repuestos sin categoría. */
data class CategoryStock(val category: ItemCategory?, val partCount: Int, val units: BigDecimal)

data class InventoryValuationUiState(
    val valuation: Money? = null,
    val parts: List<Part> = emptyList(),
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = true
) {
    val withStockCount: Int get() = parts.count { it.totalStock > BigDecimal.ZERO }
    val lowStockCount: Int get() = parts.count { it.isLowStock && !it.isOutOfStock }
    val outOfStockCount: Int get() = parts.count { it.isOutOfStock }

    /** Existencias por categoría, de más a menos unidades. El backend solo da el total en soles. */
    val byCategory: List<CategoryStock>
        get() = parts.filter { it.totalStock > BigDecimal.ZERO }
            .groupBy { it.category }
            .map { (category, list) ->
                CategoryStock(category, list.size, list.fold(BigDecimal.ZERO) { sum, p -> sum + p.totalStock })
            }
            .sortedByDescending { it.units }
}

/**
 * Valorización del inventario (`GET /inventory/items/valuation`, `inventory:batches:read`): una cifra en soles. En
 * línea: sin red no se calcula. El desglose sale de las existencias de la caché, no de valores en soles.
 */
@HiltViewModel
class InventoryValuationViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeCatalog: ObservePartsCatalogUseCase,
    private val getValuation: GetInventoryValuationUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryValuationUiState())
    val uiState: StateFlow<InventoryValuationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canRead = session?.has(Permission.BATCHES_READ) == true) }
            }
        }
        viewModelScope.launch { observeCatalog(null).collect { parts -> _uiState.update { it.copy(parts = parts) } } }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && !_uiState.value.isOnline && _uiState.value.valuation == null
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getValuation()
                .onSuccess { v -> _uiState.update { it.copy(valuation = v.total, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUiText()) } }
        }
    }
}
