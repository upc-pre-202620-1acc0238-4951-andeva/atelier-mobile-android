package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import kotlin.math.max

/**
 * Baliza de estado en vivo: punto sólido con halo pulsante en [Canvas].
 * Se detiene automáticamente cuando "quitar animaciones" del sistema está activo o [pulsing] es falso.
 */
@Composable
fun AtelierLiveBeacon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulsing: Boolean = true
) {
    val reducedMotion = rememberReducedMotion()
    val shouldPulse = pulsing && !reducedMotion

    val pulseProgress: Float = if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "liveBeacon")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = EaseOutQuint),
                repeatMode = RepeatMode.Restart
            ),
            label = "liveBeaconPulse"
        )
        animated
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .size(size * 2f)
            .clearAndSetSemantics {}
    ) {
        val coreRadius = (size.toPx() / 2f).coerceAtMost(this.size.minDimension / 2f)
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        if (shouldPulse) {
            val haloRadius = coreRadius + (coreRadius * 1.0f * pulseProgress)
            val haloAlpha = (1f - pulseProgress) * 0.45f
            drawCircle(
                color = color.copy(alpha = haloAlpha),
                radius = haloRadius,
                center = center
            )
        } else {
            drawCircle(
                color = color.copy(alpha = 0.20f),
                radius = coreRadius * 1.45f,
                center = center
            )
        }

        drawCircle(
            color = color,
            radius = coreRadius,
            center = center
        )
    }
}

/**
 * Barra métrica fina en [Canvas] con extremos redondeados, relleno degradado suave, animación por
 * resorte críticamente amortiguado ([AtelierSprings.criticallyDamped]) y marca de umbral opcional.
 */
@Composable
fun AtelierMetricBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = AtelierTheme.colors.surfaceRecessed,
    height: Dp = 6.dp,
    markerAt: Float? = null
) {
    val colors = AtelierTheme.colors
    val reducedMotion = rememberReducedMotion()
    val clampedTarget = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedTarget,
        animationSpec = if (reducedMotion) tween(0) else AtelierSprings.criticallyDamped(),
        label = "metricBarProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics {}
    ) {
        val radiusPx = this.size.height / 2f
        val cornerRadius = CornerRadius(radiusPx, radiusPx)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = this.size,
            cornerRadius = cornerRadius
        )

        if (animatedProgress > 0f) {
            val fillWidth = max(this.size.width * animatedProgress, this.size.height)
                .coerceAtMost(this.size.width)
            val fillBrush = Brush.horizontalGradient(
                colors = listOf(color.copy(alpha = 0.85f), color),
                startX = 0f,
                endX = fillWidth
            )
            drawRoundRect(
                brush = fillBrush,
                topLeft = Offset.Zero,
                size = Size(width = fillWidth, height = this.size.height),
                cornerRadius = cornerRadius
            )
        }

        if (markerAt != null) {
            val markerX = (this.size.width * markerAt.coerceIn(0f, 1f))
                .coerceIn(1.dp.toPx(), (this.size.width - 1.dp.toPx()).coerceAtLeast(1.dp.toPx()))
            drawLine(
                color = colors.inkSecondary.copy(alpha = 0.75f),
                start = Offset(x = markerX, y = 0f),
                end = Offset(x = markerX, y = this.size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// region Previews

@Composable
private fun LiveBeaconAndMetricPreviewBody() {
    val colors = AtelierTheme.colors
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AtelierLiveBeacon(color = colors.okMark, pulsing = true)
            Text("Bahía 2 · En curso", style = MaterialTheme.typography.titleMedium)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AtelierLiveBeacon(color = colors.decisionAccent, pulsing = true)
            Text("Esperando aprobación", style = MaterialTheme.typography.titleMedium)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AtelierLiveBeacon(color = colors.badMark, pulsing = false)
            Text("Sin conexión (estática)", style = MaterialTheme.typography.bodyMedium)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ocupación de bahías · 72 % (meta 85 %)", style = MaterialTheme.typography.labelLarge)
            AtelierMetricBar(
                progress = 0.72f,
                color = colors.info,
                markerAt = 0.85f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meta diaria · 100 %", style = MaterialTheme.typography.labelLarge)
            AtelierMetricBar(
                progress = 1f,
                color = colors.okMark,
                height = 8.dp
            )
        }
    }
}

@Preview(name = "Baliza y barra · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun LiveBeaconPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    LiveBeaconAndMetricPreviewBody()
}

@Preview(name = "Baliza y barra · oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LiveBeaconPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    LiveBeaconAndMetricPreviewBody()
}

// endregion
