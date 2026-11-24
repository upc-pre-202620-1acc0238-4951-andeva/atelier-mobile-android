package com.andeva.atelier.mobile.features.inventory.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.application.DispatchFifoBatchUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartBatchesUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.ReceiveDirectBatchUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartBatchesUseCase
import com.andeva.atelier.mobile.features.inventory.application.RestoreBatchUseCase
import com.andeva.atelier.mobile.features.inventory.application.UpdatePartUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * US32 E1: ficha del repuesto, lotes FIFO, edición (`PARTS_MANAGE`) y operaciones directas de lote
 * (`BATCHES_RECEIVE`, `BATCHES_DISPATCH_FIFO`, `BATCHES_RESTORE`).
 */
@HiltViewModel
class PartDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observePart: ObservePartUseCase,
    observeBatches: ObservePartBatchesUseCase,
    private val refreshBatches: RefreshPartBatchesUseCase,
    connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle,
    private val updatePart: UpdatePartUseCase,
    private val receiveDirectBatch: ReceiveDirectBatchUseCase,
    private val dispatchFifoBatch: DispatchFifoBatchUseCase,
    private val restoreBatch: RestoreBatchUseCase
) : ViewModel() {

    private val partId: String = savedStateHandle["partId"] ?: ""

    private val _uiState = MutableStateFlow(PartDetailUiState())
    val uiState: StateFlow<PartDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canReadBatches = session?.has(Permission.BATCHES_READ) == true,
                        canManageParts = session?.has(Permission.PARTS_MANAGE) == true,
                        canReceiveBatch = session?.has(Permission.BATCHES_RECEIVE) == true,
                        canDispatchFifo = session?.has(Permission.BATCHES_DISPATCH_FIFO) == true,
                        canRestoreBatch = session?.has(Permission.BATCHES_RESTORE) == true,
                        canOpenPurchaseOrders = session?.has(Permission.PURCHASE_ORDERS_READ) == true ||
                            session?.has(Permission.PURCHASE_ORDERS_CREATE) == true
                    )
                }
            }
        }
        viewModelScope.launch {
            observePart(partId).collect { part -> _uiState.update { it.copy(part = part) } }
        }
        viewModelScope.launch {
            observeBatches(partId).collect { batches ->
                // Orden FIFO defensivo: del más antiguo al más nuevo; sin fecha al final.
                val fifo = batches.sortedBy { it.arrivalDate ?: Instant.MAX }
                _uiState.update { it.copy(batches = fifo) }
            }
        }
        // D2: al volver la red tras una recarga fallida, se actualiza sola y el aviso se quita.
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && _uiState.value.showingCache
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        refresh()
    }

    /** Recarga los lotes; sin red se siguen mostrando los guardados. */
    fun refresh(force: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            refreshBatches(partId, force)
                .onSuccess { _uiState.update { it.copy(isLoading = false, showingCache = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, showingCache = true, error = e.toUiText()) }
                }
        }
    }

    fun onOpenEditPart() {
        val part = _uiState.value.part ?: return
        _uiState.update {
            it.copy(
                activeSheet = PartDetailSheet.EDIT_PART,
                editName = part.name,
                editCategory = part.category ?: ItemCategory.LUBRICANTS,
                editBasePrice = part.basePrice?.amount?.toPlainString().orEmpty(),
                editMinStock = part.minimumStock.formatQuantity(),
                editStatus = part.status ?: PartStatus.ACTIVE,
                editNameError = null,
                editPriceError = null,
                editMinStockError = null,
                sheetError = null
            )
        }
    }

    fun onOpenReceiveBatch() {
        _uiState.update {
            it.copy(
                activeSheet = PartDetailSheet.RECEIVE_BATCH,
                batchNumber = "",
                batchQuantity = "",
                batchUnitCost = "",
                batchNumberError = null,
                batchQtyError = null,
                batchCostError = null,
                sheetError = null
            )
        }
    }

    fun onOpenDispatchFifo() {
        _uiState.update {
            it.copy(
                activeSheet = PartDetailSheet.DISPATCH_FIFO,
                batchQuantity = "",
                batchQtyError = null,
                sheetError = null
            )
        }
    }

    fun onOpenRestoreBatch() {
        _uiState.update {
            it.copy(
                activeSheet = PartDetailSheet.RESTORE_BATCH,
                batchQuantity = "",
                selectedBatchId = null,
                restoreReason = "",
                batchQtyError = null,
                sheetError = null
            )
        }
    }

    fun onDismissSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(activeSheet = PartDetailSheet.NONE, sheetError = null) }
    }

    fun onEditNameChange(v: String) = _uiState.update { it.copy(editName = v, editNameError = null, sheetError = null) }
    fun onEditCategoryChange(v: ItemCategory) = _uiState.update { it.copy(editCategory = v, sheetError = null) }
    fun onEditBasePriceChange(v: String) = _uiState.update { it.copy(editBasePrice = v, editPriceError = null, sheetError = null) }
    fun onEditMinStockChange(v: String) = _uiState.update { it.copy(editMinStock = v, editMinStockError = null, sheetError = null) }
    fun onEditStatusChange(v: PartStatus) = _uiState.update { it.copy(editStatus = v, sheetError = null) }

    fun onBatchNumberChange(v: String) = _uiState.update { it.copy(batchNumber = v, batchNumberError = null, sheetError = null) }
    fun onBatchQuantityChange(v: String) = _uiState.update { it.copy(batchQuantity = v, batchQtyError = null, sheetError = null) }
    fun onBatchUnitCostChange(v: String) = _uiState.update { it.copy(batchUnitCost = v, batchCostError = null, sheetError = null) }
    fun onSelectedBatchIdChange(v: String?) = _uiState.update { it.copy(selectedBatchId = v, sheetError = null) }
    fun onRestoreReasonChange(v: String) = _uiState.update { it.copy(restoreReason = v, sheetError = null) }

    fun onConfirmEditPart() {
        val state = _uiState.value
        val part = state.part ?: return
        if (state.isSubmitting || !state.isOnline) return
        val price = state.editBasePrice.trim().replace(',', '.').toBigDecimalOrNull()
        val minStock = state.editMinStock.trim().replace(',', '.').toBigDecimalOrNull()
        val nameErr = if (!InventoryValidation.isNonEmptyText(state.editName)) R.string.inv_seg2_part_err_name else null
        val priceErr = if (price == null || !InventoryValidation.isNonNegative(price)) R.string.inv_seg2_part_err_price else null
        val minErr = if (minStock == null || !InventoryValidation.isNonNegative(minStock)) R.string.inv_seg2_part_err_min_stock else null

        if (nameErr != null || priceErr != null || minErr != null) {
            _uiState.update {
                it.copy(
                    editNameError = nameErr,
                    editPriceError = priceErr,
                    editMinStockError = minErr,
                    hapticEvent = it.hapticEvent + 1,
                    hapticIsError = true
                )
            }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, sheetError = null) }
        viewModelScope.launch {
            updatePart(
                id = part.id,
                name = state.editName,
                category = state.editCategory,
                basePrice = price!!,
                minimumStock = minStock!!,
                status = state.editStatus,
                unitOfMeasure = part.unitOfMeasure
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            activeSheet = PartDetailSheet.NONE,
                            actionBanner = UiText.Resource(R.string.inv_seg2_part_saved),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            sheetError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onConfirmReceiveBatch() {
        val state = _uiState.value
        if (state.isSubmitting || !state.isOnline) return
        val qty = state.batchQuantity.trim().replace(',', '.').toBigDecimalOrNull()
        val cost = state.batchUnitCost.trim().replace(',', '.').toBigDecimalOrNull()
        val numErr = if (!InventoryValidation.isNonEmptyText(state.batchNumber)) R.string.inv_seg2_batch_err_number else null
        val qtyErr = if (qty == null || !InventoryValidation.isPositiveQty(qty)) R.string.inv_seg2_batch_err_qty else null
        val costErr = if (cost == null || !InventoryValidation.isNonNegative(cost)) R.string.inv_seg2_batch_err_cost else null

        if (numErr != null || qtyErr != null || costErr != null) {
            _uiState.update {
                it.copy(
                    batchNumberError = numErr,
                    batchQtyError = qtyErr,
                    batchCostError = costErr,
                    hapticEvent = it.hapticEvent + 1,
                    hapticIsError = true
                )
            }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, sheetError = null) }
        viewModelScope.launch {
            receiveDirectBatch(
                itemId = partId,
                batchNumber = state.batchNumber,
                quantity = qty!!,
                unitCost = cost!!
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            activeSheet = PartDetailSheet.NONE,
                            actionBanner = UiText.Resource(R.string.inv_seg2_batch_received_ok),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            sheetError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onConfirmDispatchFifo() {
        val state = _uiState.value
        if (state.isSubmitting || !state.isOnline) return
        val qty = state.batchQuantity.trim().replace(',', '.').toBigDecimalOrNull()
        val qtyErr = if (qty == null || !InventoryValidation.isPositiveQty(qty)) R.string.inv_seg2_batch_err_qty else null
        if (qtyErr != null) {
            _uiState.update {
                it.copy(batchQtyError = qtyErr, hapticEvent = it.hapticEvent + 1, hapticIsError = true)
            }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, sheetError = null) }
        viewModelScope.launch {
            dispatchFifoBatch(itemId = partId, quantity = qty!!)
                .onSuccess { allocation ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            activeSheet = PartDetailSheet.NONE,
                            actionBanner = UiText.Resource(
                                R.string.inv_seg2_batch_dispatched_ok,
                                allocation.allocatedQuantity.formatQuantity()
                            ),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            sheetError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    fun onConfirmRestoreBatch() {
        val state = _uiState.value
        if (state.isSubmitting || !state.isOnline) return
        val qty = state.batchQuantity.trim().replace(',', '.').toBigDecimalOrNull()
        val qtyErr = if (qty == null || !InventoryValidation.isPositiveQty(qty)) R.string.inv_seg2_batch_err_qty else null
        if (qtyErr != null) {
            _uiState.update {
                it.copy(batchQtyError = qtyErr, hapticEvent = it.hapticEvent + 1, hapticIsError = true)
            }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, sheetError = null) }
        viewModelScope.launch {
            restoreBatch(
                itemId = partId,
                quantity = qty!!,
                batchId = state.selectedBatchId ?: state.batches.lastOrNull()?.id,
                reason = state.restoreReason
            )
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            activeSheet = PartDetailSheet.NONE,
                            actionBanner = UiText.Resource(
                                R.string.inv_seg2_batch_restored_ok,
                                res.restoredQuantity.formatQuantity()
                            ),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            sheetError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }
}
