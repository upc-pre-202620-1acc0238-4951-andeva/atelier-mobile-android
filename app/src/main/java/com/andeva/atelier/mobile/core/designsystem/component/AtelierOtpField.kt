package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.FieldShape
import com.andeva.atelier.mobile.core.designsystem.theme.JetBrainsMono

/**
 * Código de [length] dígitos. Es un solo campo de texto (acepta pegar y el código del SMS/correo)
 * dibujado como casillas de 64 dp; la casilla activa lleva el borde azul.
 */
@Composable
fun AtelierOtpField(
    value: String,
    onValueChange: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
    autoFocus: Boolean = true,
    onDone: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus, enabled) {
        if (autoFocus && enabled) focusRequester.requestFocus()
    }
    val progress = stringResource(R.string.auth_otp_progress, value.length, length)

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
        modifier = modifier
            .focusRequester(focusRequester)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = progress
            },
        enabled = enabled,
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interaction,
        decorationBox = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val active = focused && index == value.length.coerceAtMost(length - 1)
                    val border = when {
                        isError -> atelier.bad
                        active -> scheme.primary
                        else -> scheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(FieldShape)
                            .background(scheme.surface)
                            .border(1.5.dp, border, FieldShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value.getOrNull(index)?.toString().orEmpty(),
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Medium,
                                fontSize = 28.sp,
                                lineHeight = 36.sp
                            ),
                            color = scheme.onSurface
                        )
                    }
                }
            }
        }
    )
}
