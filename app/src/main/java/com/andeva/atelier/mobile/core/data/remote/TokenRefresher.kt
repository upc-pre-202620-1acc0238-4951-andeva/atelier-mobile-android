package com.andeva.atelier.mobile.core.data.remote

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.session.AuthRefreshService
import com.andeva.atelier.mobile.core.data.remote.session.RefreshTokenRequestDto
import com.andeva.atelier.mobile.core.di.RefreshClient
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renueva el token una sola vez aunque varias peticiones reciban 401 a la vez.
 *
 * El refresh rota el refreshToken (docs/api/README.md §2): reusar el anterior devuelve 401 y
 * cerraría la sesión. Por eso todo pasa por un `synchronized` y, si el token guardado ya no es el
 * que llevaba la petición, se devuelve el nuevo sin volver a llamar al backend.
 *
 * Bloqueante a propósito: lo usan TokenAuthenticator (hilo de OkHttp) y SyncWorker (Dispatchers.IO).
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val sessionManager: SessionManager,
    @RefreshClient private val refreshService: AuthRefreshService
) {
    private val lock = Any()

    /**
     * Devuelve un token distinto de [staleToken], renovándolo si hace falta.
     * null si no hay sesión, si el refresh fue rechazado (la sesión se borra) o si no hay red.
     */
    fun refreshIfStale(staleToken: String?): String? = synchronized(lock) {
        val current = runBlocking { sessionManager.getAuthToken() } ?: return null
        if (staleToken != null && current != staleToken) return current
        val refreshToken = runBlocking { sessionManager.getRefreshToken() } ?: return null

        val response = try {
            refreshService.refresh(RefreshTokenRequestDto(refreshToken)).execute()
        } catch (_: IOException) {
            return null                                   // sin red: se conserva la sesión
        }
        val body = response.body()
        if (response.isSuccessful && body != null) {
            runBlocking { sessionManager.save(body) }
            return body.token
        }
        if (response.code() == 401) runBlocking { sessionManager.clear() }
        null
    }

    /** Renueva antes de que venza (margen de 60 s). Lo usa SyncWorker antes de enviar la cola. */
    fun ensureFresh(nowMillis: Long = System.currentTimeMillis()): String? {
        val token = runBlocking { sessionManager.getAuthToken() } ?: return null
        val expiresAt = runBlocking { sessionManager.getTokenExpiresAt() } ?: return token
        return if (expiresAt - nowMillis > EXPIRY_MARGIN_MS) token else refreshIfStale(token)
    }

    private companion object {
        const val EXPIRY_MARGIN_MS = 60_000L
    }
}
