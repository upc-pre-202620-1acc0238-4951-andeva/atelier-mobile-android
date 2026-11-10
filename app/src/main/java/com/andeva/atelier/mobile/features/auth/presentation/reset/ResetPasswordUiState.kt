package com.andeva.atelier.mobile.features.auth.presentation.reset

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation

data class ResetPasswordUiState(
    /** Enlace completo del correo o solo el token (H1: sin deep link). */
    val token: String = "",
    val password: String = "",
    val confirmation: String = "",
    val passwordVisible: Boolean = false,
    val confirmationVisible: Boolean = false,
    val isLoading: Boolean = false,
    val done: Boolean = false,
    val error: UiText? = null,
    /** 400: el enlace venció, se usó o es inválido. */
    val tokenInvalid: Boolean = false
) {
    val hasMinLength: Boolean get() = password.length >= AuthValidation.PASSWORD_MIN
    val hasUppercase: Boolean get() = password.any(Char::isUpperCase)
    val passwordsMatch: Boolean get() = confirmation.isNotEmpty() && password == confirmation

    val canSubmit: Boolean
        get() = !isLoading && token.isNotBlank() && AuthValidation.isPassword(password) &&
            hasUppercase && passwordsMatch
}
