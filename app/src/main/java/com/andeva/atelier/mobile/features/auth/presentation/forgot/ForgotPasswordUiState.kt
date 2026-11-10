package com.andeva.atelier.mobile.features.auth.presentation.forgot

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    /** El backend aceptó la solicitud. No dice si el correo existe, así que la confirmación es siempre la misma. */
    val sent: Boolean = false,
    val error: UiText? = null
) {
    val canSubmit: Boolean get() = !isLoading && AuthValidation.isEmail(email.trim())
}
