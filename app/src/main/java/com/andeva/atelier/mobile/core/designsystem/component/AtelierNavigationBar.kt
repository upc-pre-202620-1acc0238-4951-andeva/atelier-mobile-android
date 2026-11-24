package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
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
 * Dock flotante de precisión (DESIGN.md → Navigation): cápsula [SurfaceTier.Floating] de 64 dp con 3 a 5
 * destinos, indicador animado con [AtelierSprings.dock] y orbe de acción esculpido separado a la derecha.
 * [selectedIndex] < 0 apaga el indicador.
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
    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotion()
    var pillWidthPx by remember { mutableIntStateOf(0) }
    AtelierSurface(
        modifier = modifier.height(BarHeight),
        tier = SurfaceTier.Floating,
        shape = FloatingBarShape,
        contentPadding = PaddingValues(BarPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { pillWidthPx = it.width }
        ) {
            val slot: Dp = with(density) { (pillWidthPx.toFloat() / items.size.coerceAtLeast(1)).toDp() }
            val target = slot * selectedIndex.coerceAtLeast(0)
            val offset by animateDpAsState(
                targetValue = target,
                animationSpec = if (reducedMotion) tween(0) else AtelierSprings.dock(),
                label = "barIndicator"
            )
            if (pillWidthPx > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = offset)
                        .width(slot)
                        .fillMaxHeight()
                        .alpha(if (selectedIndex < 0) 0f else 1f)
                        .clip(FloatingBarShape)
                        .background(scheme.primaryContainer)
                        .border(1.dp, scheme.primary.copy(alpha = 0.22f), FloatingBarShape)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { index, item ->
                    Slot(item, selected = index == selectedIndex, onClick = { onSelect(index) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Slot(item: AtelierNavItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = rememberReducedMotion()
    val interaction = remember { MutableInteractionSource() }
    val tint = if (selected) scheme.onPrimaryContainer else AtelierTheme.colors.inkSecondary
    val iconScale by animateFloatAsState(
        targetValue = if (selected && !reducedMotion) 1.08f else 1f,
        animationSpec = if (reducedMotion) tween(0) else AtelierSprings.snappy(),
        label = "navIconScale"
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(FloatingBarShape)
            .springPressScale(interaction)
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
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
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
    val orbBrush = remember(colors.heroStart, colors.heroEnd) {
        Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd))
    }
    Column(
        modifier = Modifier
            .size(BarHeight)
            .springPressScale(interaction)
            .clip(FloatingBarShape)
            .background(orbBrush)
            .border(1.dp, AtelierBlueSoft.copy(alpha = 0.45f), FloatingBarShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                role = Role.Button,
                onClick = action.onClick
            )
            .semantics { contentDescription = action.description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
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
