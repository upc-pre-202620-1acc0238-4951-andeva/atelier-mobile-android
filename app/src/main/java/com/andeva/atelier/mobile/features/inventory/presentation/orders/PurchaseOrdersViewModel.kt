package com.andeva.atelier.mobile.features.inventory.presentation.orders

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.CreatePurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrdersUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetSuppliersUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLineInput
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import java.math.BigDecimal
import java.math.RoundingMode
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
    val canRead: Boolean = true,
    val canCreate: Boolean = false,
    val createSheetOpen: Boolean = false,
    val suppliers: List<Supplier> = emptyList(),
    val isLoadingSuppliers: Boolean = false,
    val suppliersError: UiText? = null,
    val selectedSupplierId: String? = null,
    val catalogParts: List<Part> = emptyList(),
    val partQuery: String = "",
    val lowStockOnly: Boolean = false,
    val selectedPartId: String? = null,
    val lineQuantity: String = "1",
    val lineUnitCost: String = "",
    @StringRes val lineQtyError: Int? = null,
    @StringRes val lineCostError: Int? = null,
    val orderNumber: String = "",
    val notes: String = "",
    val isCreating: Boolean = false,
    val createError: UiText? = null,
    val createdOrderId: String? = null,
    val hapticEvent: Int = 0,
    val hapticIsError: Boolean = false
) {
    val lowStockParts: List<Part>
        get() = catalogParts
            .filter { it.isLowStock && it.status != PartStatus.DISCONTINUED && it.status != PartStatus.INACTIVE }
            .sortedWith(compareByDescending<Part> { it.isOutOfStock }.thenBy { it.totalStock }.thenBy { it.name.lowercase() })

    val filteredCatalogParts: List<Part>
        get() {
            val base = if (lowStockOnly) lowStockParts else catalogParts
            val q = partQuery.trim().lowercase()
            val matched = if (q.isEmpty()) base else base.filter {
                it.name.lowercase().contains(q) || it.sku.lowercase().contains(q)
            }
            return matched
                .sortedWith(compareByDescending<Part> { it.isOutOfStock }.thenByDescending { it.isLowStock }.thenBy { it.name.lowercase() })
                .take(20)
        }
}

internal fun suggestedReplenishmentQty(part: Part): BigDecimal {
    if (!part.isLowStock) return BigDecimal.ONE
    val target = part.minimumStock.multiply(BigDecimal("2"))
    val needed = (target - part.totalStock).setScale(0, RoundingMode.CEILING)
    return needed.max(BigDecimal.ONE)
}

/** US34 E2: órdenes de compra por estado + alta de borrador (`PURCHASE_ORDERS_CREATE`). En línea (06 §2). */
@HiltViewModel
class PurchaseOrdersViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOrders: GetPurchaseOrdersUseCase,
    private val getSuppliers: GetSuppliersUseCase,
    private val createPurchaseOrder: CreatePurchaseOrderUseCase,
    connectivity: ConnectivityObserver,
    private val observeCatalog: ObservePartsCatalogUseCase? = null,
    private val refreshCatalog: RefreshPartsCatalogUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseOrdersUiState())
    val uiState: StateFlow<PurchaseOrdersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canRead = session?.has(Permission.PURCHASE_ORDERS_READ) == true,
                        canCreate = session?.has(Permission.PURCHASE_ORDERS_CREATE) == true
                    )
                }
            }
        }
        if (observeCatalog != null) {
            viewModelScope.launch {
                observeCatalog.invoke(null).collect { parts ->
                    _uiState.update { state ->
                        val nextPart = state.selectedPartId?.let { id -> parts.firstOrNull { it.id == id } }
                            ?: parts.firstOrNull()
                        state.copy(
                            catalogParts = parts,
                            selectedPartId = nextPart?.id,
                            lineUnitCost = state.lineUnitCost.ifBlank {
                                nextPart?.basePrice?.amount?.toPlainString().orEmpty()
                            }
                        )
                    }
                }
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

    fun onOpenCreateSheet() {
        _uiState.update {
            val firstPart = it.filteredCatalogParts.firstOrNull() ?: it.catalogParts.firstOrNull()
            it.copy(
                createSheetOpen = true,
                selectedSupplierId = null,
                partQuery = "",
                lowStockOnly = false,
                selectedPartId = firstPart?.id,
                lineQuantity = firstPart?.let { p -> suggestedReplenishmentQty(p).stripTrailingZeros().toPlainString() } ?: "1",
                lineUnitCost = firstPart?.basePrice?.amount?.toPlainString() ?: "0.00",
                lineQtyError = null,
                lineCostError = null,
                orderNumber = "",
                notes = "",
                createError = null,
                suppliersError = null,
                isLoadingSuppliers = true
            )
        }
        viewModelScope.launch { refreshCatalog?.invoke(force = false) }
        viewModelScope.launch {
            getSuppliers()
                .onSuccess { list ->
                    val active = list.filter { it.active }
                    val display = active.ifEmpty { list }
                    _uiState.update {
                        it.copy(
                            suppliers = display,
                            isLoadingSuppliers = false,
                            selectedSupplierId = display.firstOrNull()?.id
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingSuppliers = false, suppliersError = e.toUiText()) }
                }
        }
    }

    fun onDismissCreateSheet() {
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(createSheetOpen = false, createError = null) }
    }

    fun onSelectSupplier(supplierId: String) = _uiState.update {
        it.copy(selectedSupplierId = supplierId, createError = null)
    }

    fun onPartQueryChange(v: String) = _uiState.update { it.copy(partQuery = v, createError = null) }

    fun onToggleLowStockOnly() = _uiState.update { state ->
        val nextOnly = !state.lowStockOnly
        val updated = state.copy(lowStockOnly = nextOnly, createError = null)
        val visible = updated.filteredCatalogParts
        val keepSelected = visible.any { it.id == updated.selectedPartId }
        val nextPart = if (keepSelected) null else visible.firstOrNull()
        if (nextPart != null) {
            updated.copy(
                selectedPartId = nextPart.id,
                lineQuantity = suggestedReplenishmentQty(nextPart).stripTrailingZeros().toPlainString(),
                lineUnitCost = nextPart.basePrice?.amount?.toPlainString() ?: updated.lineUnitCost.ifBlank { "0.00" },
                lineQtyError = null,
                lineCostError = null
            )
        } else {
            updated
        }
    }

    fun onSelectPart(part: Part) = _uiState.update {
        it.copy(
            selectedPartId = part.id,
            lineQuantity = suggestedReplenishmentQty(part).stripTrailingZeros().toPlainString(),
            lineUnitCost = part.basePrice?.amount?.toPlainString() ?: it.lineUnitCost.ifBlank { "0.00" },
            lineQtyError = null,
            lineCostError = null,
            createError = null
        )
    }

    /** Crea un borrador con TODOS los repuestos en o debajo del stock mínimo y cantidades sugeridas. */
    fun onCreateLowStockOrder() {
        val state = _uiState.value
        val supplierId = state.selectedSupplierId ?: return
        val lowStock = state.lowStockParts
        if (lowStock.isEmpty() || state.isCreating || !state.isOnline) return

        val items = lowStock.map { part ->
            PurchaseOrderLineInput(
                itemId = part.id,
                quantity = suggestedReplenishmentQty(part),
                unitCost = part.basePrice?.amount ?: BigDecimal.ZERO
            )
        }
        submitOrder(supplierId, items, state.orderNumber, state.notes)
    }

    fun onLineQuantityChange(v: String) = _uiState.update {
        it.copy(lineQuantity = v, lineQtyError = null, createError = null)
    }

    fun onLineUnitCostChange(v: String) = _uiState.update {
        it.copy(lineUnitCost = v, lineCostError = null, createError = null)
    }

    fun onOrderNumberChange(v: String) = _uiState.update { it.copy(orderNumber = v, createError = null) }

    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v, createError = null) }

    fun onCreateOrder() {
        val state = _uiState.value
        val supplierId = state.selectedSupplierId ?: return
        if (state.isCreating || !state.isOnline) return

        val items: List<PurchaseOrderLineInput> = if (state.selectedPartId != null) {
            val qty = state.lineQuantity.trim().replace(',', '.').toBigDecimalOrNull()
            val cost = state.lineUnitCost.trim().replace(',', '.').toBigDecimalOrNull()
            val qtyErr = if (qty == null || !InventoryValidation.isPositiveQty(qty)) R.string.inv_seg2_batch_err_qty else null
            val costErr = if (cost == null || !InventoryValidation.isNonNegative(cost)) R.string.inv_seg2_batch_err_cost else null
            if (qtyErr != null || costErr != null) {
                _uiState.update {
                    it.copy(
                        lineQtyError = qtyErr,
                        lineCostError = costErr,
                        hapticEvent = it.hapticEvent + 1,
                        hapticIsError = true
                    )
                }
                return
            }
            listOf(PurchaseOrderLineInput(state.selectedPartId, qty!!, cost!!))
        } else if (observeCatalog != null) {
            _uiState.update {
                it.copy(
                    createError = UiText.Resource(R.string.inv_seg2_po_no_parts),
                    hapticEvent = it.hapticEvent + 1,
                    hapticIsError = true
                )
            }
            return
        } else {
            emptyList()
        }

        submitOrder(supplierId, items, state.orderNumber, state.notes)
    }

    private fun submitOrder(
        supplierId: String,
        items: List<PurchaseOrderLineInput>,
        orderNumber: String,
        notes: String
    ) {
        _uiState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            createPurchaseOrder(
                supplierId = supplierId,
                items = items,
                orderNumber = orderNumber,
                notes = notes
            )
                .onSuccess { created ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createSheetOpen = false,
                            createdOrderId = created.id,
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                    refresh()
                }
                .onFailure { e ->
                    val err = if (e is MissingBranchException) {
                        UiText.Resource(R.string.inv_seg2_po_missing_branch)
                    } else {
                        e.toUiText()
                    }
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createError = err,
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onCreatedOrderNavigated() {
        _uiState.update { it.copy(createdOrderId = null) }
    }
}
