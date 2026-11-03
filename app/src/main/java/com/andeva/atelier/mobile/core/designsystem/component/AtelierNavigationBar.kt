package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import kotlin.math.roundToInt

/** Pestaña de [AtelierNavigationBar]. [badge] > 0 dibuja un contador sobre el ícono. */
data class AtelierNavItem(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0
)

/**
 * Barra inferior M3 de 80 dp. En vez de un fondo por pestaña, una sola "lámpara" (píldora con una barra de luz
 * arriba) se desliza hasta la pestaña activa en 380 ms con ease-out; con "quitar animaciones" salta directo.
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
    val position by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, items.lastIndex).toFloat(),
        animationSpec = if (reduced) snap() else tween(durationMillis = 380, easing = EaseOutQuint),
        label = "navLamp"
    )

    Column(modifier = modifier.fillMaxWidth().background(scheme.surface).navigationBarsPadding()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)
        ) {
            val itemWidth = maxWidth / items.size
            val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }

            // Lámpara: píldora de 32 dp + barra de luz de 24×3 dp sobre el borde superior.
            Box(
                modifier = Modifier
                    .offset { IntOffset((itemWidthPx * position).roundToInt(), 0) }
                    .width(itemWidth)
                    .height(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.primaryContainer)
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset((itemWidthPx * position).roundToInt(), 0) }
                    .width(itemWidth)
                    .height(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(width = 24.dp, height = 3.dp)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(atelier.primaryText)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .selectable(
                                selected = selected,
                                onClick = { onSelect(index) },
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = null
                            )
                            .pressScale(interaction),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val tint = if (selected) scheme.onSurface else atelier.muted
                        Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                            if (item.badge > 0) {
                                BadgedBox(badge = { Badge { Text(item.badge.coerceAtMost(99).toString()) } }) {
                                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            }
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
