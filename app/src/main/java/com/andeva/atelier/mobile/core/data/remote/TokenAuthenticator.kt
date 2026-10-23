package com.andeva.atelier.mobile.core.data.remote

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/** Se ejecuta solo ante un 401: renueva el token una vez y repite la petición. */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (request.header(RETRY_HEADER) != null) return null              // ya se intentó
        if (PublicEndpoints.isPublic(request)) return null                 // 401 de credenciales, no de sesión
        val sentToken = request.header("Authorization")?.removePrefix("Bearer ")
        val token = tokenRefresher.refreshIfStale(sentToken) ?: return null
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .header(RETRY_HEADER, "1")
            .build()
    }

    companion object {
        const val RETRY_HEADER = "X-Retry-After-Refresh"
    }
}
