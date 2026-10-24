package com.andeva.atelier.mobile.features.main.application

import com.andeva.atelier.mobile.features.main.domain.PendingOperationRepository
import javax.inject.Inject

/** Lista de la pantalla "Pendientes de sincronizar". */
class ObservePendingOperationsUseCase @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository
) {
    operator fun invoke() = pendingOperationRepository.observeAll()
}
