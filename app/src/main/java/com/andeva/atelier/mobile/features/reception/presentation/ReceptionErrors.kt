package com.andeva.atelier.mobile.features.reception.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException

/** Campo que `ReceptionValidation` rechazó antes de llamar al backend, para marcarlo en el formulario. */
internal fun Throwable.invalidReceptionField(): String? = (this as? InvalidReceptionInputException)?.field

/** Dato inválido: aviso genérico (el campo se marca aparte); el resto muestra el `message` del backend. */
internal fun Throwable.toReceptionUiText(): UiText = when (this) {
    is InvalidReceptionInputException -> UiText.Resource(R.string.reception_invalid_input)
    else -> toUiText()
}
