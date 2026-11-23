package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueDark
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape

/**
 * [Filled] es la acción principal: una por pantalla. [OnPanel] va sobre el panel azul.
 * [Destructive] es contorno y etiqueta en tinta de error; [DestructiveFilled] solo en la confirmación final.
 */
enum class AtelierButtonStyle { Filled, Tonal, Outlined, Text, OnPanel, Destructive, DestructiveFilled }

/**
 * Botón rectangular de 12 dp de radio y 48 dp de alto mínimo. Responde al tocar, no al soltar.
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
    icon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }

    val container: Color = when (style) {
        AtelierButtonStyle.Filled -> scheme.primary
        AtelierButtonStyle.Tonal -> scheme.primaryContainer
        AtelierButtonStyle.OnPanel -> atelier.onPanel
        AtelierButtonStyle.DestructiveFilled -> scheme.error
        else -> Color.Transparent
    }
    val content: Color = when (style) {
        AtelierButtonStyle.Filled -> scheme.onPrimary
        AtelierButtonStyle.Tonal -> scheme.onPrimaryContainer
        AtelierButtonStyle.OnPanel -> AtelierBlueDark
        AtelierButtonStyle.DestructiveFilled -> scheme.onError
        AtelierButtonStyle.Destructive -> atelier.bad
        AtelierButtonStyle.Outlined -> scheme.onSurface
        AtelierButtonStyle.Text -> atelier.primaryText
    }
    val border = when (style) {
        AtelierButtonStyle.Outlined -> BorderStroke(1.dp, scheme.outline)
        AtelierButtonStyle.Destructive -> BorderStroke(1.dp, atelier.bad)
        else -> null
    }
    val padding = if (style == AtelierButtonStyle.Text) PaddingValues(horizontal = 12.dp) else PaddingValues(horizontal = 24.dp)

    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(interaction)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics { role = Role.Button },
        shape = ControlShape,
        color = container,
        contentColor = content,
        border = border,
        interactionSource = interaction
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
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
            Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
