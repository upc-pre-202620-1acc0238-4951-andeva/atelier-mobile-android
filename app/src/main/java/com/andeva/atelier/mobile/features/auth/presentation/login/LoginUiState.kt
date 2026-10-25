package com.andeva.atelier.mobile.features.auth.presentation.login

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    /** Llegó desde la verificación del correo: se muestra el aviso de éxito. */
    val emailVerified: Boolean = false,
    /** Un solo uso: el backend rechazó el acceso porque el correo no está verificado. */
    val unverifiedEmail: String? = null
) {
    val canSubmit: Boolean
        get() = !isLoading && AuthValidation.isEmail(email.trim()) && password.isNotEmpty()
}
