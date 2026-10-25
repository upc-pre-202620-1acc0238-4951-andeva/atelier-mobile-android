package com.andeva.atelier.mobile.features.auth.presentation.register

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation
import com.andeva.atelier.mobile.features.auth.domain.InvitationPreview

/** Pasos de A3: pegar el enlace → formulario, o invitación vencida. */
enum class RegisterStep { Link, Form, Gone }

/** Por qué la invitación ya no sirve: 410 vencida, 404 inexistente o revocada. */
enum class InvitationProblem { Expired, NotFound }

data class RegisterUiState(
    val step: RegisterStep = RegisterStep.Link,
    val linkInput: String = "",
    val invitation: InvitationPreview? = null,
    val problem: InvitationProblem? = null,
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null
) {
    val hasMinLength: Boolean get() = password.length >= AuthValidation.PASSWORD_MIN
    val hasUppercase: Boolean get() = password.any(Char::isUpperCase)
    val passwordsMatch: Boolean get() = confirmPassword.isNotEmpty() && password == confirmPassword

    val canValidate: Boolean get() = !isLoading && linkInput.isNotBlank()

    val canSubmit: Boolean
        get() = step == RegisterStep.Form && !isLoading &&
            AuthValidation.isName(firstName) && AuthValidation.isName(lastName) &&
            AuthValidation.isPassword(password) && hasUppercase && passwordsMatch
}
