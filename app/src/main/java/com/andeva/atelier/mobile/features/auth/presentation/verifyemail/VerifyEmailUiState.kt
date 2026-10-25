package com.andeva.atelier.mobile.features.auth.presentation.verifyemail

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation

data class VerifyEmailUiState(
    val email: String,
    val code: String = "",
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val error: UiText? = null,
    val info: UiText? = null,
    /** Segundos para poder pedir otro código. */
    val resendSecondsLeft: Int = RESEND_COOLDOWN_SECONDS,
    /** Segundos de vida del código. `null` hasta que la app envía uno (no se sabe cuándo se mandó el anterior). */
    val expiresSecondsLeft: Int? = null,
    val verified: Boolean = false
) {
    val canSubmit: Boolean get() = !isVerifying && !isResending && AuthValidation.isOtp(code)
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isVerifying && !isResending

    /** `a•••••@taller.pe`: el correo no se muestra completo en pantalla. */
    val maskedEmail: String
        get() {
            val local = email.substringBefore('@')
            val domain = email.substringAfter('@', "")
            val head = local.take(1)
            return "$head•••••@$domain"
        }

    companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
        /** El OTP vive 15 minutos (docs/03-traceability.md US02). */
        const val CODE_LIFETIME_SECONDS = 15 * 60
    }
}
