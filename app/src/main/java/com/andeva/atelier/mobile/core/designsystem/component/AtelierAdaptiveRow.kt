package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contenedor adaptable por medición intrínseca de ancho: coloca los hijos en fila horizontal con [horizontalSpacing]
 * cuando sus anchos máximos intrínsecos caben dentro del ancho disponible; cuando exceden (pantallas compactas de
 * 320–360 dp, textos largos en español o `fontScale = 1.3f`) los apila verticalmente con [verticalSpacing] a ancho
 * completo, sin recortar ni depender de umbrales mágicos de `fontScale`.
 */
@Composable
fun AtelierAdaptiveRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    equalWidthInRow: Boolean = false,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(constraints.minWidth, constraints.minHeight) {}

        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val totalIntrinsicWidth = measurables.sumOf { it.maxIntrinsicWidth(constraints.maxHeight) } +
            hGap * (measurables.size - 1)
        val fitsHorizontally = maxWidth != Int.MAX_VALUE && totalIntrinsicWidth <= maxWidth

        if (fitsHorizontally) {
            val itemConstraints = if (equalWidthInRow) {
                val cellWidth = ((maxWidth - hGap * (measurables.size - 1)) / measurables.size).coerceAtLeast(0)
                constraints.copy(minWidth = cellWidth, maxWidth = cellWidth, minHeight = 0)
            } else {
                constraints.copy(minWidth = 0, minHeight = 0)
            }
            val placeables = measurables.map { it.measure(itemConstraints) }
            val height = placeables.maxOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
            val width = if (equalWidthInRow) {
                maxWidth
            } else {
                (placeables.sumOf { it.width } + hGap * (placeables.size - 1))
                    .coerceIn(constraints.minWidth, constraints.maxWidth)
            }
            layout(width, height) {
                var x = 0
                placeables.forEach { placeable ->
                    val y = (height - placeable.height) / 2
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGap
                }
            }
        } else {
            val childConstraints = constraints.copy(
                minWidth = if (maxWidth != Int.MAX_VALUE) maxWidth else constraints.minWidth,
                maxWidth = maxWidth,
                minHeight = 0
            )
            val placeables = measurables.map { it.measure(childConstraints) }
            val height = (placeables.sumOf { it.height } + vGap * (placeables.size - 1))
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            val width = placeables.maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
            layout(width, height) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + vGap
                }
            }
        }
    }
}
