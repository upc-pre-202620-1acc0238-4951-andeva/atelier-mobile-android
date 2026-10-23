package com.andeva.atelier.mobile.core.notifications

import com.andeva.atelier.mobile.core.data.remote.apiCallUnit
import com.andeva.atelier.mobile.core.data.remote.session.DeviceTokenService
import com.andeva.atelier.mobile.core.data.remote.session.RegisterDeviceTokenRequestDto
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Token FCM del dispositivo (docs/04-mobile-integration.md §2):
 * tras iniciar sesión se registra; al cerrar sesión se borra (`fcmToken: null`) antes de `sign-out`.
 */
@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val service: DeviceTokenService,
    private val gson: Gson
) {
    /** Lo llama el login. Sin `google-services.json` Firebase no está inicializado y no se registra nada. */
    suspend fun registerCurrentToken(): Result<Unit> {
        val token = currentFcmToken() ?: return Result.failure(IllegalStateException("FCM token unavailable"))
        return register(token)
    }

    suspend fun register(fcmToken: String): Result<Unit> =
        apiCallUnit(gson) { service.register(RegisterDeviceTokenRequestDto(fcmToken)) }

    suspend fun clear(): Result<Unit> =
        apiCallUnit(gson) { service.register(RegisterDeviceTokenRequestDto(null)) }

    private suspend fun currentFcmToken(): String? = try {
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }
    } catch (_: IllegalStateException) {
        null
    }
}
