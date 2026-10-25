package com.andeva.atelier.mobile.core.designsystem.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.domain.ApiException

/**
 * Texto que el ViewModel decide pero no puede resolver (no tiene `Context`): el `message` del
 * backend ([Dynamic]) o un recurso de `strings.xml` ([Resource]).
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    class Resource(@StringRes val id: Int, vararg val args: Any) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args)
    }
}

/** Mensaje del backend si lo hay (AGENTS.md); si no, el error genérico o el de "sin conexión". */
fun Throwable.toUiText(): UiText = when {
    this is ApiException && message.isNotBlank() -> UiText.Dynamic(message)
    this is java.io.IOException -> UiText.Resource(R.string.error_requires_connection)
    else -> UiText.Resource(R.string.error_generic)
}
