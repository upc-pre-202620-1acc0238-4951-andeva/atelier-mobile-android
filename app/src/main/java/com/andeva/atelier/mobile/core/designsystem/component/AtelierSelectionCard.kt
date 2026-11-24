package com.andeva.atelier.mobile.core.designsystem.component

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.PlateShape

/**
 * Tarjeta de selección compacta para diálogos y hojas: usa [SurfaceTier.Card] cuando está sin
 * seleccionar y eleva con borde azul luminoso y sombra suave al seleccionarse.
 */
@Composable
fun AtelierSelectionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    price: String? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    val minTouch = LocalAtelierMinTouchTarget.current
    val contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)

    if (selected) {
        val interaction = remember { MutableInteractionSource() }
        val selectedBorder = remember(scheme.primary) {
            Brush.verticalGradient(listOf(AtelierBlueSoft, scheme.primary))
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = minTouch)
                .then(if (enabled) Modifier.springPressScale(interaction) else Modifier.alpha(0.38f))
                .clip(ControlShape)
                .background(atelier.card, ControlShape)
                .background(scheme.primaryContainer.copy(alpha = 0.42f), ControlShape)
                .border(1.5.dp, selectedBorder, ControlShape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(contentPadding)
        ) {
            SelectionCardContent(
                title = title,
                subtitle = subtitle,
                price = price,
                badgeText = badgeText,
                badgeColor = badgeColor,
                leadingIcon = leadingIcon,
                selected = true
            )
        }
    } else {
        AtelierSurface(
            modifier = modifier.fillMaxWidth().heightIn(min = minTouch),
            tier = SurfaceTier.Card,
            shape = ControlShape,
            onClick = onClick,
            enabled = enabled,
            contentPadding = contentPadding
        ) {
            SelectionCardContent(
                title = title,
                subtitle = subtitle,
                price = price,
                badgeText = badgeText,
                badgeColor = badgeColor,
                leadingIcon = leadingIcon,
                selected = false
            )
        }
    }
}

@Composable
private fun SelectionCardContent(
    title: String,
    subtitle: String?,
    price: String?,
    badgeText: String?,
    badgeColor: Color?,
    leadingIcon: ImageVector?,
    selected: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leadingIcon != null) {
            Surface(
                shape = CircleShape,
                color = if (selected) scheme.primary else atelier.surfaceRecessed,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = PlateShape,
                        color = badgeColor ?: scheme.secondaryContainer,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = atelier.inkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!price.isNullOrBlank()) {
            Text(
                text = price,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = atelier.primaryText
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = atelier.primaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
