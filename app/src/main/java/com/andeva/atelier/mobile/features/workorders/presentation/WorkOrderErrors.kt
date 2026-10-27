package com.andeva.atelier.mobile.features.workorders.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException

/** Campo que `WorkOrderValidation` rechazó antes de llamar al backend, para marcarlo en el formulario. */
internal fun Throwable.invalidWorkOrderField(): String? = (this as? InvalidWorkOrderInputException)?.field

/** Dato inválido: aviso genérico (el campo se marca aparte); el resto muestra el `message` del backend. */
internal fun Throwable.toWorkOrderUiText(): UiText = when (this) {
    is InvalidWorkOrderInputException -> UiText.Resource(R.string.workorder_invalid_input)
    else -> toUiText()
}
