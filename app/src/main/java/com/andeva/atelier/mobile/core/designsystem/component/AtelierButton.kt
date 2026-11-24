package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueDark
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierError
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape

/**
 * [Filled] es la acción principal: una por pantalla. [OnPanel] va sobre el panel azul.
 * [Destructive] es contorno y etiqueta en tinta de error; [DestructiveFilled] solo en la confirmación final.
 */
enum class AtelierButtonStyle { Filled, Tonal, Outlined, Text, OnPanel, Destructive, DestructiveFilled }

/**
 * Botón dimensional esculpido de 12 dp de radio y altura táctil ambiental ([LocalAtelierMinTouchTarget]:
 * 48 dp estándar, 56 dp en piso/fosa). Responde al tocar con resorte críticamente amortiguado ([springPressScale]).
 *
 * Con [loading] conserva su ancho, muestra progreso junto a la etiqueta y no responde.
 * Deshabilitado baja a 38 % de opacidad: quien lo usa debe mostrar el motivo cerca.
 */
@Composable
fun AtelierButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AtelierButtonStyle = AtelierButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    minHeight: Dp = LocalAtelierMinTouchTarget.current
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val interactive = enabled && !loading

    val content: Color = when (style) {
        AtelierButtonStyle.Filled -> Color.White
        AtelierButtonStyle.Tonal -> scheme.onPrimaryContainer
        AtelierButtonStyle.OnPanel -> AtelierBlueDark
        AtelierButtonStyle.DestructiveFilled -> Color.White
        AtelierButtonStyle.Destructive -> atelier.bad
        AtelierButtonStyle.Outlined -> scheme.onSurface
        AtelierButtonStyle.Text -> atelier.primaryText
    }

    val backgroundModifier = when (style) {
        AtelierButtonStyle.Filled -> Modifier.background(
            brush = Brush.verticalGradient(listOf(atelier.buttonTop, atelier.buttonBottom)),
            shape = ControlShape
        )
        AtelierButtonStyle.DestructiveFilled -> Modifier.background(
            brush = Brush.verticalGradient(listOf(atelier.destructiveTop, atelier.destructiveBottom)),
            shape = ControlShape
        )
        AtelierButtonStyle.Tonal -> Modifier.background(scheme.primaryContainer, ControlShape)
        AtelierButtonStyle.Outlined -> Modifier.background(atelier.card, ControlShape)
        AtelierButtonStyle.OnPanel -> Modifier.background(atelier.onPanel, ControlShape)
        AtelierButtonStyle.Destructive,
        AtelierButtonStyle.Text -> Modifier
    }

    val border: BorderStroke? = when (style) {
        AtelierButtonStyle.Filled -> BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(AtelierBlueSoft.copy(alpha = 0.35f), AtelierBlueSoft.copy(alpha = 0.08f))
            )
        )
        AtelierButtonStyle.Tonal -> BorderStroke(1.dp, scheme.primary.copy(alpha = 0.18f))
        AtelierButtonStyle.Outlined -> BorderStroke(
            width = 1.dp,
            brush = if (atelier.isDark) {
                Brush.verticalGradient(listOf(atelier.specularTop, atelier.specularBottom))
            } else {
                Brush.verticalGradient(listOf(scheme.outline.copy(alpha = 0.50f), scheme.outline.copy(alpha = 0.85f)))
            }
        )
        AtelierButtonStyle.Destructive -> BorderStroke(1.dp, atelier.bad)
        AtelierButtonStyle.DestructiveFilled -> BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        AtelierButtonStyle.OnPanel,
        AtelierButtonStyle.Text -> null
    }

    val padding = when {
        style == AtelierButtonStyle.Text -> PaddingValues(horizontal = 12.dp)
        density.fontScale >= 1.2f -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        else -> PaddingValues(horizontal = 20.dp)
    }

    val rippleColor = when (style) {
        AtelierButtonStyle.Filled,
        AtelierButtonStyle.DestructiveFilled -> Color.White
        else -> Color.Unspecified
    }

    CompositionLocalProvider(LocalContentColor provides content) {
        Box(
            modifier = modifier
                .heightIn(min = minHeight)
                .then(if (interactive) Modifier.springPressScale(interaction) else Modifier)
                .alpha(if (enabled) 1f else 0.38f)
                .clip(ControlShape)
                .then(backgroundModifier)
                .then(if (border != null) Modifier.border(border, ControlShape) else Modifier)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = rippleColor),
                    enabled = interactive,
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = minHeight)
                    .padding(padding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = content,
                        strokeWidth = 2.dp
                    )
                    icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// region Previews

@Composable
private fun ButtonPreviewBody() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AtelierButton(
            text = "Guardar orden",
            icon = Icons.Outlined.Check,
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AtelierButton(
                text = "Tonal",
                style = AtelierButtonStyle.Tonal,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            AtelierButton(
                text = "Cancelar",
                style = AtelierButtonStyle.Outlined,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AtelierButton(
                text = "Eliminar",
                icon = Icons.Outlined.DeleteOutline,
                style = AtelierButtonStyle.Destructive,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            AtelierButton(
                text = "Confirmar",
                style = AtelierButtonStyle.DestructiveFilled,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(name = "Botones · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun ButtonPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { ButtonPreviewBody() }

@Preview(
    name = "Botones · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ButtonPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { ButtonPreviewBody() }

// endregion
