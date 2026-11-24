package com.andeva.atelier.mobile.features.inventory.presentation.orders

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.AddPurchaseOrderItemUseCase
import com.andeva.atelier.mobile.features.inventory.application.CancelPurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.IssuePurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.ReceivePurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PurchaseOrderDetailUiState(
    val order: PurchaseOrder? = null,
    /** Nombre del repuesto por id, desde la caché del catálogo; la línea solo trae el id. */
    val partNames: Map<String, String> = emptyMap(),
    val catalogParts: List<Part> = emptyList(),
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = true,
    val canCreate: Boolean = false,
    val canReceive: Boolean = false,
    val actionBanner: UiText? = null,
    val actionError: UiText? = null,
    val isMutating: Boolean = false,
    // Hoja agregar línea (DRAFT)
    val showAddLineSheet: Boolean = false,
    val partQuery: String = "",
    val selectedPartId: String? = null,
    val lineQuantity: String = "",
    val lineUnitCost: String = "",
    @StringRes val lineQtyError: Int? = null,
    @StringRes val lineCostError: Int? = null,
    val addLineError: UiText? = null,
    val isAddingLine: Boolean = false,
    // Hoja anular OC (DRAFT o ISSUED)
    val showCancelSheet: Boolean = false,
    val cancelReason: String = "",
    val cancelError: UiText? = null,
    val isCanceling: Boolean = false,
    // Hoja recibir OC (ISSUED, H15)
    val showReceiveSheet: Boolean = false,
    val receiptNumber: String = "",
    val photo: File? = null,
    val isReceiving: Boolean = false,
    /** `message` del backend tal cual (H15: puede ser un 400 genérico aunque la orden ya estuviera recibida). */
    val receiveError: UiText? = null,
    val justReceived: Boolean = false,
    val hapticEvent: Int = 0,
    val hapticIsError: Boolean = false
) {
    val canConfirmReceive: Boolean
        get() = receiptNumber.isNotBlank() && photo != null && isOnline && !isReceiving

    val filteredCatalogParts: List<Part>
        get() {
            val q = partQuery.trim().lowercase()
            val matched = if (q.isEmpty()) catalogParts else catalogParts.filter {
                it.name.lowercase().contains(q) || it.sku.lowercase().contains(q)
            }
            return matched
                .sortedWith(compareByDescending<Part> { it.isOutOfStock }.thenByDescending { it.isLowStock }.thenBy { it.name.lowercase() })
                .take(20)
        }
}

/**
 * US34 E2: orden de compra con ciclo completo: agregar líneas (`DRAFT`), emitir (`ISSUED`), anular (`CANCELED`)
 * y recibir con comprobante fotográfico (`BATCHES_RECEIVE`, H15).
 */
@HiltViewModel
class PurchaseOrderDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOrder: GetPurchaseOrderUseCase,
    private val observePart: ObservePartUseCase,
    private val receiveOrder: ReceivePurchaseOrderUseCase,
    connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle,
    observeCatalog: ObservePartsCatalogUseCase,
    private val refreshCatalog: RefreshPartsCatalogUseCase,
    private val addPurchaseOrderItem: AddPurchaseOrderItemUseCase,
    private val issuePurchaseOrder: IssuePurchaseOrderUseCase,
    private val cancelPurchaseOrder: CancelPurchaseOrderUseCase
) : ViewModel() {

    private val orderId: String = savedStateHandle["purchaseOrderId"] ?: ""

    private val _uiState = MutableStateFlow(PurchaseOrderDetailUiState())
    val uiState: StateFlow<PurchaseOrderDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canRead = session?.has(Permission.PURCHASE_ORDERS_READ) == true,
                        canCreate = session?.has(Permission.PURCHASE_ORDERS_CREATE) == true,
                        canReceive = session?.has(Permission.BATCHES_RECEIVE) == true
                    )
                }
            }
        }
        viewModelScope.launch {
            observeCatalog(null).collect { parts ->
                _uiState.update { it.copy(catalogParts = parts) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && !_uiState.value.isOnline && _uiState.value.order == null
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getOrder(orderId)
                .onSuccess { order ->
                    _uiState.update { it.copy(order = order, isLoading = false) }
                    loadPartNames(order)
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUiText()) } }
        }
    }

    private suspend fun loadPartNames(order: PurchaseOrder) {
        val names = order.lines.map { it.itemId }.distinct()
            .mapNotNull { id -> observePart(id).firstOrNull()?.name?.let { id to it } }
            .toMap()
        _uiState.update { it.copy(partNames = names) }
    }

    fun onOpenAddLine() {
        _uiState.update {
            it.copy(
                showAddLineSheet = true,
                partQuery = "",
                selectedPartId = it.catalogParts.firstOrNull()?.id,
                lineQuantity = "",
                lineUnitCost = it.catalogParts.firstOrNull()?.basePrice?.amount?.toPlainString().orEmpty(),
                lineQtyError = null,
                lineCostError = null,
                addLineError = null
            )
        }
        viewModelScope.launch { refreshCatalog(force = false) }
    }

    fun onDismissAddLine() {
        if (_uiState.value.isAddingLine) return
        _uiState.update { it.copy(showAddLineSheet = false, addLineError = null) }
    }

    fun onPartQueryChange(v: String) = _uiState.update { it.copy(partQuery = v) }

    fun onSelectPart(part: Part) {
        _uiState.update {
            it.copy(
                selectedPartId = part.id,
                lineUnitCost = it.lineUnitCost.ifBlank { part.basePrice?.amount?.toPlainString().orEmpty() },
                addLineError = null
            )
        }
    }

    fun onLineQuantityChange(v: String) = _uiState.update { it.copy(lineQuantity = v, lineQtyError = null, addLineError = null) }

    fun onLineUnitCostChange(v: String) = _uiState.update { it.copy(lineUnitCost = v, lineCostError = null, addLineError = null) }

    fun onConfirmAddLine() {
        val state = _uiState.value
        val itemId = state.selectedPartId ?: return
        if (state.isAddingLine || !state.isOnline) return
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
        _uiState.update { it.copy(isAddingLine = true, addLineError = null) }
        viewModelScope.launch {
            addPurchaseOrderItem(orderId, itemId, qty!!, cost!!)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            order = updated,
                            isAddingLine = false,
                            showAddLineSheet = false,
                            actionBanner = UiText.Resource(R.string.inv_seg2_po_line_added_ok),
                            actionError = null,
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                    loadPartNames(updated)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isAddingLine = false,
                            addLineError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onIssueOrder() {
        val state = _uiState.value
        if (state.isMutating || !state.isOnline) return
        _uiState.update { it.copy(isMutating = true, actionError = null, actionBanner = null) }
        viewModelScope.launch {
            issuePurchaseOrder(orderId)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            order = updated,
                            isMutating = false,
                            actionBanner = UiText.Resource(R.string.inv_seg2_po_issued_ok),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onOpenCancel() = _uiState.update { it.copy(showCancelSheet = true, cancelReason = "", cancelError = null) }

    fun onDismissCancel() {
        if (_uiState.value.isCanceling) return
        _uiState.update { it.copy(showCancelSheet = false, cancelError = null) }
    }

    fun onCancelReasonChange(v: String) = _uiState.update { it.copy(cancelReason = v, cancelError = null) }

    fun onConfirmCancel() {
        val state = _uiState.value
        if (state.isCanceling || !state.isOnline) return
        _uiState.update { it.copy(isCanceling = true, cancelError = null) }
        viewModelScope.launch {
            cancelPurchaseOrder(orderId, state.cancelReason)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            order = updated,
                            isCanceling = false,
                            showCancelSheet = false,
                            actionBanner = UiText.Resource(R.string.inv_seg2_po_canceled_ok),
                            actionError = null,
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isCanceling = false,
                            cancelError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onOpenReceive() = _uiState.update { it.copy(showReceiveSheet = true, receiveError = null) }

    fun onDismissReceive() {
        if (_uiState.value.isReceiving) return
        _uiState.update { it.copy(showReceiveSheet = false, receiptNumber = "", photo = null, receiveError = null) }
    }

    fun onReceiptNumberChange(value: String) = _uiState.update { it.copy(receiptNumber = value, receiveError = null) }

    fun onPhotoTaken(file: File) = _uiState.update { it.copy(photo = file, receiveError = null) }

    fun onConfirmReceive() {
        val state = _uiState.value
        if (!state.canConfirmReceive) return
        _uiState.update { it.copy(isReceiving = true, receiveError = null) }
        viewModelScope.launch {
            receiveOrder(orderId, state.receiptNumber.trim(), state.photo)
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            order = order, isReceiving = false, showReceiveSheet = false, receiptNumber = "",
                            photo = null, justReceived = true,
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isReceiving = false,
                            receiveError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }
}
