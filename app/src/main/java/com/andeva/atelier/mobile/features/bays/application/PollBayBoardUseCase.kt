package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Flujo frío: refresca al empezar y cada [POLL_INTERVAL] mientras alguien lo recolecta; al cancelar se detiene.
 * Si hay [ConnectivityObserver], pausa el sondeo mientras el dispositivo esté sin red y aplica backoff en fallos.
 * Sin `operations:bays:read` termina sin emitir ni llamar al backend.
 */
class PollBayBoardUseCase @Inject constructor(
    private val bayRepository: BayRepository,
    private val sessionManager: SessionManager,
    private val connectivity: ConnectivityObserver? = null
) {
    operator fun invoke(): Flow<Result<Unit>> = flow {
        if (sessionManager.getSession()?.has(Permission.BAYS_READ) != true) return@flow
        var currentDelay = POLL_INTERVAL
        while (true) {
            if (connectivity != null && !connectivity.isOnline.value) {
                connectivity.isOnline.first { it }
            }
            val result = bayRepository.refreshBranchBays()
            emit(result)
            currentDelay = if (result.isSuccess) {
                POLL_INTERVAL
            } else {
                (currentDelay * 2).coerceAtMost(MAX_BACKOFF_INTERVAL)
            }
            delay(currentDelay)
        }
    }

    companion object {
        val POLL_INTERVAL: Duration = 30.seconds
        val MAX_BACKOFF_INTERVAL: Duration = 120.seconds
    }
}
