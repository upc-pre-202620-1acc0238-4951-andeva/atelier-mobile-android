package com.andeva.atelier.mobile.features.main.application

import com.andeva.atelier.mobile.features.main.domain.PendingOperationRepository
import javax.inject.Inject

/** Contador del ícono de pendientes. */
class ObservePendingOperationCountUseCase @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository
) {
    operator fun invoke() = pendingOperationRepository.observeCount()
}
