package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Flujo frío: refresca al empezar y cada [POLL_INTERVAL] mientras alguien lo recolecta; al cancelar se detiene.
 * Emite el resultado de cada refresco. Sin `operations:bays:read` termina sin emitir ni llamar al backend.
 */
class PollBayBoardUseCase @Inject constructor(
    private val bayRepository: BayRepository,
    private val sessionManager: SessionManager
) {
    operator fun invoke(): Flow<Result<Unit>> = flow {
        if (sessionManager.getSession()?.has(Permission.BAYS_READ) != true) return@flow
        while (true) {
            emit(bayRepository.refreshBranchBays())
            delay(POLL_INTERVAL)
        }
    }

    companion object {
        val POLL_INTERVAL: Duration = 30.seconds
    }
}
