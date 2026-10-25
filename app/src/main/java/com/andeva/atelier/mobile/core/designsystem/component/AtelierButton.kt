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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.PillShape

enum class AtelierButtonStyle { Filled, Tonal, Outlined, Text }

/**
 * Botón píldora. 56 dp en las acciones principales (se usa con guantes) y 48 dp el de texto.
 * Con [loading] muestra un indicador y no responde, como deshabilitado.
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
    val active = enabled && !loading
    val interaction = remember { MutableInteractionSource() }

    val container: Color = when {
        style == AtelierButtonStyle.Text || style == AtelierButtonStyle.Outlined -> Color.Transparent
        !active -> atelier.idleContainer
        style == AtelierButtonStyle.Filled -> scheme.primary
        else -> scheme.primaryContainer
    }
    val content: Color = when {
        !active -> atelier.muted
        style == AtelierButtonStyle.Filled -> scheme.onPrimary
        style == AtelierButtonStyle.Tonal -> scheme.onPrimaryContainer
        else -> atelier.primaryText
    }
    val minHeight = if (style == AtelierButtonStyle.Text) 48.dp else 56.dp
    val padding = if (style == AtelierButtonStyle.Text) PaddingValues(horizontal = 12.dp) else PaddingValues(horizontal = 24.dp)

    Surface(
        onClick = onClick,
        enabled = active,
        modifier = modifier
            .heightIn(min = minHeight)
            .pressScale(interaction)
            .semantics { role = Role.Button },
        shape = PillShape,
        color = container,
        contentColor = content,
        border = if (style == AtelierButtonStyle.Outlined) BorderStroke(1.dp, scheme.outline) else null,
        interactionSource = interaction
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
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Text(
                text = text,
                style = if (style == AtelierButtonStyle.Text) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.titleMedium
            )
        }
    }
}
