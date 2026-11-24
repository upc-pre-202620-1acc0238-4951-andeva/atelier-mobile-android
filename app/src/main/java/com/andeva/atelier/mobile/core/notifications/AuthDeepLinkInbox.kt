package com.andeva.atelier.mobile.core.notifications

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Enlace web entrante de autenticación (US05 restablecer contraseña / US01 aceptar invitación). */
sealed interface AuthDeepLink {
    val token: String

    data class ResetPassword(override val token: String) : AuthDeepLink
    data class AcceptInvitation(override val token: String) : AuthDeepLink
}

/**
 * Buzón de App Links de autenticación publicados desde `MainActivity` (`onCreate`/`onNewIntent`) y consumidos
 * por `AppNavHost`. Si ya existe sesión activa, `AppNavHost` descarta el enlace y avisa al usuario.
 */
@Singleton
class AuthDeepLinkInbox @Inject constructor() {
    private val _pending = MutableStateFlow<AuthDeepLink?>(null)
    val pending: StateFlow<AuthDeepLink?> = _pending.asStateFlow()

    fun postFromUri(uri: Uri?): Boolean {
        val link = parseUri(uri) ?: return false
        _pending.value = link
        return true
    }

    fun consume(): AuthDeepLink? = _pending.value.also { _pending.value = null }

    companion object {
        private const val EXPECTED_HOST = "atelier.andeva.tech"
        private const val MAX_TOKEN_LENGTH = 512

        fun parseUri(uri: Uri?): AuthDeepLink? {
            if (uri == null) return null
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (!uri.host.equals(EXPECTED_HOST, ignoreCase = true)) return null
            val path = uri.path.orEmpty()
            val rawToken = uri.getQueryParameter("token")?.trim()?.take(MAX_TOKEN_LENGTH).orEmpty()
            return when {
                path.startsWith("/auth/reset-password") -> AuthDeepLink.ResetPassword(rawToken)
                path.startsWith("/auth/accept-invitation") -> AuthDeepLink.AcceptInvitation(rawToken)
                else -> null
            }
        }
    }
}
