package com.andeva.atelier.mobile.features.main.application

import com.andeva.atelier.mobile.features.main.domain.PendingOperationRepository
import javax.inject.Inject

/** Acción *Descartar*. */
class DiscardPendingOperationUseCase @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository
) {
    suspend operator fun invoke(id: String) = pendingOperationRepository.discard(id)
}
