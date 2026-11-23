package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.FloatingBarShape

/** Destino de [AtelierNavigationBar]. [badge] > 0 dibuja un contador sobre el ícono. */
data class AtelierNavItem(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0
)

/** Botón circular separado de la píldora: la acción más importante de la pantalla (p. ej. "Recibir vehículo"). */
data class AtelierBarAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Alto que la barra flotante tapa del contenido (píldora, márgenes e inset del sistema). Lo publica `AppNavHost`;
 * vale 0 cuando la barra no está a la vista. Las raíces de pestaña lo suman al final de su contenido con
 * [floatingBarPadding] para que la última fila pueda subir por encima de la píldora.
 */
val LocalFloatingBarInset = compositionLocalOf { 0.dp }

@Composable
fun Modifier.floatingBarPadding(): Modifier = padding(bottom = LocalFloatingBarInset.current)

private val BarHeight = 64.dp
private val BarMargin = 12.dp
private val BarPadding = 6.dp

/** La escala de fuente del sistema se detiene aquí dentro: a 1,3 cuatro etiquetas no caben en 360 dp. */
private const val BAR_MAX_FONT_SCALE = 1.15f

/**
 * Barra inferior flotante (DESIGN.md → Barra flotante): una píldora de 64 dp con 3 a 5 destinos y, aparte, un botón
 * circular de acción. El indicador del destino activo se desliza (250 ms, ease-out-expo) salvo con "quitar
 * animaciones". Plana: se separa del contenido con borde y relleno opaco, no con sombra. [selectedIndex] < 0 apaga
 * el indicador.
 */
@Composable
fun AtelierNavigationBar(
    items: List<AtelierNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    action: AtelierBarAction? = null
) {
    if (items.isEmpty()) return
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = minOf(density.fontScale, BAR_MAX_FONT_SCALE))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = BarMargin, end = BarMargin, bottom = BarMargin),
            horizontalArrangement = Arrangement.spacedBy(BarMargin),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill(items, selectedIndex, onSelect, Modifier.weight(1f))
            if (action != null) ActionButton(action)
        }
    }
}

@Composable
private fun Pill(items: List<AtelierNavItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = rememberReducedMotion()
    BoxWithConstraints(
        modifier = modifier
            .height(BarHeight)
            .clip(FloatingBarShape)
            .background(AtelierTheme.colors.card)
            .border(BorderStroke(1.dp, scheme.outlineVariant), FloatingBarShape)
            .padding(BarPadding)
    ) {
        val slot: Dp = maxWidth / items.size
        val target = slot * selectedIndex.coerceAtLeast(0)
        val offset by animateDpAsState(
            targetValue = target,
            animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 250, easing = EaseOutExpo),
            label = "barIndicator"
        )
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(slot)
                .fillMaxHeight()
                .alpha(if (selectedIndex < 0) 0f else 1f)
                .clip(FloatingBarShape)
                .background(scheme.primaryContainer)
        )
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEachIndexed { index, item ->
                Slot(item, selected = index == selectedIndex, onClick = { onSelect(index) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Slot(item: AtelierNavItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val tint = if (selected) scheme.onPrimaryContainer else AtelierTheme.colors.inkSecondary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(FloatingBarShape)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(
            badge = {
                if (item.badge > 0) {
                    Badge(containerColor = scheme.error, contentColor = scheme.onError) {
                        Text(item.badge.coerceAtMost(99).toString())
                    }
                }
            }
        ) { Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(action: AtelierBarAction) {
    val colors = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .size(BarHeight)
            .pressScale(interaction)
            .clip(FloatingBarShape)
            .background(colors.fab)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = action.onClick)
            .semantics { contentDescription = action.description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(action.icon, contentDescription = null, tint = colors.onFab, modifier = Modifier.size(24.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onFab,
            maxLines = 1
        )
    }
}

// region Previews

@Composable
private fun BarPreview() {
    val items = listOf(
        AtelierNavItem("Hoy", Icons.Outlined.Today),
        AtelierNavItem("Órdenes", Icons.Outlined.AssignmentTurnedIn),
        AtelierNavItem("Equipo", Icons.Outlined.Groups),
        AtelierNavItem("Módulos", Icons.Outlined.GridView, badge = 2)
    )
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = 24.dp)) {
        AtelierNavigationBar(
            items = items,
            selectedIndex = 0,
            onSelect = {},
            action = AtelierBarAction("Recibir", "Recibir vehículo", Icons.Outlined.DirectionsCar) {}
        )
        AtelierNavigationBar(items = items, selectedIndex = 3, onSelect = {})
    }
}

@Preview(name = "Barra · claro", widthDp = 360, showBackground = true)
@Composable
private fun BarPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { BarPreview() }

@Preview(name = "Barra · oscuro", widthDp = 360, showBackground = true, backgroundColor = 0xFF262626, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { BarPreview() }

// endregion
