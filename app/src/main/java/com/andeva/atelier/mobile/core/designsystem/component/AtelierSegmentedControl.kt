package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.PlateShape
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SegmentCapsuleShape = PlateShape

/**
 * Control segmentado sobre pista hundida ([SurfaceTier.Recessed]) con cápsula activa elevada que se
 * desliza con resorte sin rebote ([AtelierSprings.dock]). Se adapta automáticamente a escala de fuente
 * grande (>= 1.15x) o más de 3 opciones pasando a un carril horizontal desplazable sin recortar texto.
 */
@Composable
fun <T> AtelierSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    badge: ((T) -> Int)? = null
) {
    if (options.isEmpty()) return

    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotion()
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val useScrollableLayout = options.size > 3 || density.fontScale >= 1.15f

    AtelierSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        tier = SurfaceTier.Recessed,
        shape = ControlShape,
        contentPadding = PaddingValues(4.dp)
    ) {
        if (!useScrollableLayout) {
            var trackWidthPx by remember { mutableIntStateOf(0) }
            val segmentWidth = with(density) { (trackWidthPx.toFloat() / options.size).toDp() }
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = if (reducedMotion) tween(0) else AtelierSprings.dock(),
                label = "segmentIndicatorOffset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .onSizeChanged { trackWidthPx = it.width }
            ) {
                if (trackWidthPx > 0) {
                    SegmentCapsule(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(segmentWidth)
                            .fillMaxHeight()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    options.forEach { option ->
                        val isSelected = option == options[selectedIndex]
                        SegmentOptionItem(
                            text = label(option),
                            badgeCount = badge?.invoke(option) ?: 0,
                            isSelected = isSelected,
                            onClick = { onSelect(option) },
                            allowEllipsis = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()
            val itemBounds = remember(options) { mutableStateMapOf<Int, Pair<Int, Int>>() }
            val indicatorX = remember { Animatable(0f) }
            val indicatorW = remember { Animatable(0f) }
            var hasInitialized by remember { mutableStateOf(false) }

            val targetBounds = itemBounds[selectedIndex]
            LaunchedEffect(selectedIndex, targetBounds, reducedMotion) {
                val (targetX, targetW) = targetBounds ?: return@LaunchedEffect
                if (!hasInitialized || reducedMotion) {
                    indicatorX.snapTo(targetX.toFloat())
                    indicatorW.snapTo(targetW.toFloat())
                    hasInitialized = true
                } else {
                    val spec = AtelierSprings.dock<Float>()
                    launch { indicatorX.animateTo(targetX.toFloat(), spec) }
                    launch { indicatorW.animateTo(targetW.toFloat(), spec) }
                }

                val targetCenter = targetX + targetW / 2
                val viewportCenter = scrollState.viewportSize / 2
                if (viewportCenter > 0) {
                    val scrollTarget = (targetCenter - viewportCenter).coerceIn(0, scrollState.maxValue)
                    if (reducedMotion) {
                        scrollState.scrollTo(scrollTarget)
                    } else {
                        scrollState.animateScrollTo(scrollTarget)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .height(IntrinsicSize.Min)
            ) {
                if (indicatorW.value > 0f) {
                    SegmentCapsule(
                        modifier = Modifier
                            .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                            .width(with(density) { indicatorW.value.toDp() })
                            .fillMaxHeight()
                    )
                }

                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        val isSelected = index == selectedIndex
                        SegmentOptionItem(
                            text = label(option),
                            badgeCount = badge?.invoke(option) ?: 0,
                            isSelected = isSelected,
                            onClick = { onSelect(option) },
                            allowEllipsis = false,
                            modifier = Modifier
                                .fillMaxHeight()
                                .onPlaced { coords ->
                                    itemBounds[index] =
                                        coords.positionInParent().x.roundToInt() to coords.size.width
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentCapsule(modifier: Modifier = Modifier) {
    val colors = AtelierTheme.colors
    val specularBrush = remember(colors) {
        Brush.verticalGradient(listOf(colors.specularTop, colors.specularBottom))
    }
    Box(
        modifier = modifier
            .clip(SegmentCapsuleShape)
            .background(colors.surfaceElevated, SegmentCapsuleShape)
            .border(BorderStroke(1.dp, specularBrush), SegmentCapsuleShape)
    )
}

@Composable
private fun SegmentOptionItem(
    text: String,
    badgeCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    allowEllipsis: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    val textColor by animateColorAsState(
        targetValue = if (isSelected) scheme.onSurface else colors.inkSecondary,
        animationSpec = tween(durationMillis = 120, easing = EaseOutQuint),
        label = "segmentTextColor"
    )

    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(SegmentCapsuleShape)
            .springPressScale(interactionSource, pressed = 0.97f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { selected = isSelected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = if (allowEllipsis) TextOverflow.Ellipsis else TextOverflow.Visible
        )

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(PlateShape)
                    .background(
                        color = if (isSelected) {
                            colors.infoContainer
                        } else {
                            colors.outlineSoft.copy(alpha = if (colors.isDark) 0.75f else 1f)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.primaryText else colors.inkSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// region Previews

@Composable
private fun SegmentedControlPreviewBody() {
    var selected3 by remember { mutableStateOf("En curso") }
    var selected5 by remember { mutableStateOf("Todas") }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AtelierSegmentedControl(
            options = listOf("En curso", "Pausadas", "Listas"),
            selected = selected3,
            onSelect = { selected3 = it },
            label = { it },
            badge = {
                when (it) {
                    "En curso" -> 4
                    "Pausadas" -> 2
                    else -> 0
                }
            }
        )

        AtelierSegmentedControl(
            options = listOf("Todas", "En curso", "En pausa", "Asignadas", "Suspendidas"),
            selected = selected5,
            onSelect = { selected5 = it },
            label = { it },
            badge = { if (it == "Suspendidas") 1 else 0 }
        )
    }
}

@Preview(name = "SegmentedControl · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun SegmentedControlPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    SegmentedControlPreviewBody()
}

@Preview(name = "SegmentedControl · oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SegmentedControlPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    SegmentedControlPreviewBody()
}

// endregion
