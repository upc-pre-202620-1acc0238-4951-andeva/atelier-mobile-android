package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/** Pestaña de [AtelierNavigationBar]. [badge] > 0 dibuja un contador sobre el ícono. */
data class AtelierNavItem(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0
)

/**
 * Barra inferior flotante estilo cápsula (Lemon Cash style).
 * Flota sobre el contenido con bordes completamente redondeados, sombra suave,
 * sin etiquetas de texto e indicador activo circular animado con micro-interacción.
 */
@Composable
fun AtelierNavigationBar(
    items: List<AtelierNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    val reduced = rememberReducedMotion()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = scheme.primary.copy(alpha = 0.08f)),
            shape = CircleShape,
            color = scheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, atelier.outlineSoft.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interaction = remember { MutableInteractionSource() }

                    val indicatorAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = if (reduced) snap() else tween(180),
                        label = "navIndicatorAlpha"
                    )
                    val indicatorScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.7f,
                        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.65f),
                        label = "navIndicatorScale"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (selected) scheme.onPrimaryContainer else atelier.muted,
                        animationSpec = if (reduced) snap() else tween(180),
                        label = "iconColor"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1.0f,
                        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.65f),
                        label = "iconScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                onClick = { onSelect(index) },
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = null
                            )
                            .pressScale(interaction),
                        contentAlignment = Alignment.Center
                    ) {
                        // Indicador circular activo perfectamente centrado detrás del ícono
                        if (indicatorAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        alpha = indicatorAlpha
                                        scaleX = indicatorScale
                                        scaleY = indicatorScale
                                    }
                                    .clip(CircleShape)
                                    .background(scheme.primaryContainer)
                            )
                        }

                        if (item.badge > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = scheme.error,
                                        contentColor = scheme.onError
                                    ) {
                                        Text(item.badge.coerceAtMost(99).toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier
                                        .size(23.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(23.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
