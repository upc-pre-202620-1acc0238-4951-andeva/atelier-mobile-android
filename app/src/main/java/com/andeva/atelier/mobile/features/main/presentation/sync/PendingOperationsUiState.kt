package com.andeva.atelier.mobile.features.main.presentation.sync

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.main.domain.PendingOperation
import com.andeva.atelier.mobile.features.main.domain.PendingOperationStatus

data class PendingOperationsUiState(
    val operations: List<PendingOperation> = emptyList(),
    val isOnline: Boolean = true,
    val isSyncingNow: Boolean = false,
    /** Room ya respondió: antes de eso la lista se ve como esqueleto, no como "todo sincronizado". */
    val hasLoaded: Boolean = false,
    val notice: UiText? = null
) {
    val pendingCount: Int get() = operations.count { it.status == PendingOperationStatus.PENDING }
    val syncingCount: Int get() = operations.count { it.status == PendingOperationStatus.SYNCING }
    val failedCount: Int get() = operations.count { it.status == PendingOperationStatus.FAILED }
}
