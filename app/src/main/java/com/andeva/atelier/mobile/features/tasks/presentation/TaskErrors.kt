package com.andeva.atelier.mobile.features.tasks.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException

/** Campo que `TaskValidation` rechazó antes de llamar al backend, para marcarlo en el formulario. */
internal fun Throwable.invalidTaskField(): String? = (this as? InvalidTaskInputException)?.field

/** Dato inválido: aviso genérico (el campo se marca aparte); el resto muestra el `message` del backend. */
internal fun Throwable.toTaskUiText(): UiText = when (this) {
    is InvalidTaskInputException -> UiText.Resource(R.string.task_invalid_input)
    else -> toUiText()
}
