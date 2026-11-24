package com.andeva.atelier.mobile.features.inventory.presentation.detail

import androidx.annotation.StringRes
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import java.math.BigDecimal

enum class PartDetailSheet { NONE, EDIT_PART, RECEIVE_BATCH, DISPATCH_FIFO, RESTORE_BATCH }

/**
 * US32: repuesto con sus lotes en orden FIFO (el más antiguo primero). [nextBatchId] es el lote del que saldrá el
 * próximo descargo: el primero con saldo.
 */
data class PartDetailUiState(
    val part: Part? = null,
    val batches: List<Batch> = emptyList(),
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    /** La recarga de lotes falló (sin red): se muestran los guardados. */
    val showingCache: Boolean = false,
    val error: UiText? = null,
    val canReadBatches: Boolean = false,
    val canManageParts: Boolean = false,
    val canReceiveBatch: Boolean = false,
    val canDispatchFifo: Boolean = false,
    val canRestoreBatch: Boolean = false,
    val canOpenPurchaseOrders: Boolean = false,
    val actionBanner: UiText? = null,
    val activeSheet: PartDetailSheet = PartDetailSheet.NONE,
    val isSubmitting: Boolean = false,
    val sheetError: UiText? = null,
    // Formulario editar repuesto
    val editName: String = "",
    val editCategory: ItemCategory = ItemCategory.LUBRICANTS,
    val editBasePrice: String = "",
    val editMinStock: String = "",
    val editStatus: PartStatus = PartStatus.ACTIVE,
    @StringRes val editNameError: Int? = null,
    @StringRes val editPriceError: Int? = null,
    @StringRes val editMinStockError: Int? = null,
    // Formularios de lote FIFO
    val batchNumber: String = "",
    val batchQuantity: String = "",
    val batchUnitCost: String = "",
    val selectedBatchId: String? = null,
    val restoreReason: String = "",
    @StringRes val batchNumberError: Int? = null,
    @StringRes val batchQtyError: Int? = null,
    @StringRes val batchCostError: Int? = null,
    val hapticEvent: Int = 0,
    val hapticIsError: Boolean = false
) {
    val nextBatchId: String? get() = batches.firstOrNull { it.isActive }?.id

    /** Existencias que suman los lotes con saldo. */
    val batchesRemaining: BigDecimal
        get() = batches.filter { it.isActive }.fold(BigDecimal.ZERO) { sum, batch -> sum + batch.remainingQty }

    val hasAnyBatchAction: Boolean
        get() = canReceiveBatch || canDispatchFifo || canRestoreBatch
}
