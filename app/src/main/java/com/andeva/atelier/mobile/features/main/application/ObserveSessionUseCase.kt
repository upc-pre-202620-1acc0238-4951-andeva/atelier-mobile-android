package com.andeva.atelier.mobile.features.main.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import javax.inject.Inject

/**
 * Sesión actual (null al salir). Con ella se arman las pestañas por permiso (`session.has(...)`)
 * y el aviso de suscripción (`session.hasSubscriptionAccess()`). Cambia en cada refresh.
 */
class ObserveSessionUseCase @Inject constructor(
    private val sessionManager: SessionManager
) {
    operator fun invoke() = sessionManager.session
}
