package com.andeva.atelier.mobile.features.inventory.presentation.detail

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.Part
import java.math.BigDecimal

/**
 * US32: repuesto con sus lotes en orden FIFO (el más antiguo primero). [nextBatchId] es el lote del que saldrá el
 * próximo descargo: el primero con saldo.
 */
data class PartDetailUiState(
    val part: Part? = null,
    val batches: List<Batch> = emptyList(),
    val isLoading: Boolean = true,
    /** La recarga de lotes falló (sin red): se muestran los guardados. */
    val showingCache: Boolean = false,
    val error: UiText? = null,
    val canReadBatches: Boolean = false
) {
    val nextBatchId: String? get() = batches.firstOrNull { it.isActive }?.id

    /** Existencias que suman los lotes con saldo. */
    val batchesRemaining: BigDecimal
        get() = batches.filter { it.isActive }.fold(BigDecimal.ZERO) { sum, batch -> sum + batch.remainingQty }
}
