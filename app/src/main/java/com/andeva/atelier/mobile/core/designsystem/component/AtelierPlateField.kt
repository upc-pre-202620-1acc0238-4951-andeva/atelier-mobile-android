package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme

/**
 * Campo dedicado a placas vehiculares (DESIGN.md → Inputs / Fields): teclado en mayúsculas y formato
 * automático `ABC-123` mientras el usuario teclea.
 */
@Composable
fun AtelierPlateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "BKL-482",
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Search,
    onImeAction: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    AtelierTextField(
        label = label,
        value = value,
        onValueChange = { raw -> onValueChange(formatPlateInput(raw)) },
        modifier = modifier,
        leadingIcon = Icons.Outlined.DirectionsCar,
        trailing = trailing,
        placeholder = placeholder,
        isError = isError,
        errorText = errorText,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() })
    )
}

internal fun formatPlateInput(raw: String): String {
    val clean = raw.uppercase().filter { it.isLetterOrDigit() }.take(7)
    return if (clean.length > 3) "${clean.take(3)}-${clean.drop(3)}" else clean
}

@Preview(name = "Campo placa · claro", widthDp = 390, showBackground = true)
@Composable
private fun PlateFieldPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AtelierPlateField(
        label = "Placa del vehículo",
        value = "BKL-482",
        onValueChange = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    )
}

@Preview(
    name = "Campo placa · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PlateFieldPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AtelierPlateField(
        label = "Placa del vehículo",
        value = "F4T-119",
        onValueChange = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    )
}
