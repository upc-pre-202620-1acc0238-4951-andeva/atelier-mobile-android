package com.andeva.atelier.mobile.features.inventory.presentation.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.ReceivePurchaseOrderUseCase
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
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = true,
    val canReceive: Boolean = false,
    val showReceiveSheet: Boolean = false,
    val receiptNumber: String = "",
    val photo: File? = null,
    val isReceiving: Boolean = false,
    /** `message` del backend tal cual (H15: puede ser un 400 genérico aunque la orden ya estuviera recibida). */
    val receiveError: UiText? = null,
    val justReceived: Boolean = false
) {
    val canConfirmReceive: Boolean
        get() = receiptNumber.isNotBlank() && photo != null && isOnline && !isReceiving
}

/** US34 E2: orden de compra con sus líneas y recepción (`BATCHES_RECEIVE`) confirmada en hoja inferior. */
@HiltViewModel
class PurchaseOrderDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOrder: GetPurchaseOrderUseCase,
    private val observePart: ObservePartUseCase,
    private val receiveOrder: ReceivePurchaseOrderUseCase,
    connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle
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
                        canReceive = session?.has(Permission.BATCHES_RECEIVE) == true
                    )
                }
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
                            photo = null, justReceived = true
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isReceiving = false, receiveError = e.toUiText()) } }
        }
    }
}
