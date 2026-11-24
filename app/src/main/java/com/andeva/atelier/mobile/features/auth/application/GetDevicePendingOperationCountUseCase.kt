package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import javax.inject.Inject

/**
 * Conteo neutro de operaciones pendientes guardadas en el dispositivo para advertir en `LoginScreen`
 * cuando una sesión expiró o se cerró con trabajo local sin sincronizar (US04, docs/06-offline-sync.md).
 */
class GetDevicePendingOperationCountUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Int = repository.devicePendingOperationCount()
}
