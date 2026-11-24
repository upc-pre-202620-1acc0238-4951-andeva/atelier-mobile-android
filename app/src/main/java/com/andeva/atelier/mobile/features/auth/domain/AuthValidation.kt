package com.andeva.atelier.mobile.features.auth.domain

import java.net.URLDecoder

/**
 * Restricciones de los modelos de request de docs/api/iam.md. Solo evitan una llamada inútil;
 * el backend vuelve a validar todo.
 */
object AuthValidation {
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 64
    const val NAME_MAX = 100
    const val OTP_LENGTH = 6
    const val TOKEN_MAX_LENGTH = 512

    private val phonePattern = Regex("^\\+?[0-9]{9,15}$")
    private val otpPattern = Regex("^[0-9]{$OTP_LENGTH}$")

    fun isEmail(value: String): Boolean {
        val trimmed = value.trim()
        val atIndex = trimmed.indexOf('@')
        if (atIndex <= 0 || atIndex != trimmed.lastIndexOf('@')) return false
        val local = trimmed.substring(0, atIndex)
        val domain = trimmed.substring(atIndex + 1)
        if (local.isBlank() || local.startsWith('.') || local.endsWith('.') || local.contains("..")) return false
        if (domain.isBlank() || domain.startsWith('.') || domain.endsWith('.') || domain.contains("..")) return false
        val parts = domain.split('.')
        if (parts.size < 2 || parts.any { it.isBlank() || it.startsWith('-') || it.endsWith('-') }) return false
        val tld = parts.last()
        return tld.length >= 2 && tld.all { it.isLetterOrDigit() }
    }

    fun isPassword(value: String) = value.length in PASSWORD_MIN..PASSWORD_MAX

    fun isName(value: String) = value.isNotBlank() && value.length <= NAME_MAX

    fun isPhone(value: String) = phonePattern.matches(value)

    fun isOtp(value: String) = otpPattern.matches(value)

    /**
     * Token de invitación o de restablecimiento. Acepta el token directo o el enlace completo del correo
     * (`…/auth/accept-invitation?token=…` o `…/auth/reset-password?token=…`): de una URL se toma `token`.
     * Si la URL trae `?token=` vacío o malformado, devuelve cadena vacía sin lanzar excepción.
     */
    fun tokenFrom(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return ""
        val hasQuery = text.contains('?')
        val query = text.substringAfter('?', "").substringBefore('#')
        val tokenParam = query.split('&').firstOrNull { it.startsWith("token=") }
        if (tokenParam != null) {
            val raw = tokenParam.substringAfter('=')
            val decoded = runCatching { URLDecoder.decode(raw, Charsets.UTF_8.name()) }.getOrDefault(raw)
            return decoded.trim().take(TOKEN_MAX_LENGTH)
        }
        if (hasQuery && (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true))) {
            return ""
        }
        return text.take(TOKEN_MAX_LENGTH)
    }
}
