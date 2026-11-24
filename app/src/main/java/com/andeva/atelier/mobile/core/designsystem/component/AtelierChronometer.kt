package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Estado visual del cronómetro de labor efectiva. Nunca depende solo del color. */
enum class ChronometerVisualState { IDLE, RUNNING, PAUSED, SUSPENDED }

/**
 * Cronómetro de labor efectiva en fosa sobre [SurfaceTier.Elevated] con marcas geométricas de
 * precisión en [Canvas] y [AtelierLiveBeacon] activo cuando está en marcha. Cifra única en Display
 * (`tnum` Satoshi Black) sin saltos horizontales ni animaciones distractoras.
 */
@Composable
fun AtelierChronometer(
    elapsedText: String,
    statusLabel: String,
    visualState: ChronometerVisualState,
    modifier: Modifier = Modifier,
    caption: String? = null,
    loading: Boolean = false
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val isRunning = visualState == ChronometerVisualState.RUNNING
    val (chipKind, icon) = when (visualState) {
        ChronometerVisualState.RUNNING -> ChipKind.Info to Icons.Outlined.PlayCircle
        ChronometerVisualState.PAUSED -> ChipKind.Warn to Icons.Outlined.PauseCircle
        ChronometerVisualState.SUSPENDED -> ChipKind.Bad to Icons.Outlined.WarningAmber
        ChronometerVisualState.IDLE -> ChipKind.Idle to Icons.Outlined.Schedule
    }
    val dialColor = if (isRunning) {
        colors.primaryText.copy(alpha = 0.14f)
    } else {
        colors.inkSecondary.copy(alpha = 0.10f)
    }

    AtelierSurface(
        modifier = modifier.fillMaxWidth(),
        tier = SurfaceTier.Elevated,
        shape = HeroShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$statusLabel, $elapsedText${caption?.let { ", $it" }.orEmpty()}"
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val radius = size.height * 0.85f
                val center = Offset(x = size.width * 0.92f, y = size.height * 0.82f)
                drawArc(
                    color = dialColor,
                    startAngle = 185f,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
                )
                val tickSteps = 12
                for (i in 0..tickSteps) {
                    val angleRad = (185.0 + (95.0 / tickSteps) * i) * (PI / 180.0)
                    val isMajor = i % 3 == 0
                    val innerR = radius - (if (isMajor) 8.dp.toPx() else 4.dp.toPx())
                    val start = Offset(
                        x = center.x + (innerR * cos(angleRad)).toFloat(),
                        y = center.y + (innerR * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        x = center.x + (radius * cos(angleRad)).toFloat(),
                        y = center.y + (radius * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = dialColor,
                        start = start,
                        end = end,
                        strokeWidth = (if (isMajor) 1.5.dp else 1.dp).toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(16.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AtelierChip(
                        text = statusLabel,
                        kind = chipKind,
                        icon = icon,
                        live = isRunning && !loading,
                        modifier = Modifier.bone(loading, widthFraction = 0.4f)
                    )
                    if (!caption.isNullOrBlank()) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkSecondary,
                            modifier = Modifier.bone(loading, widthFraction = 0.35f)
                        )
                    }
                }
                Text(
                    text = elapsedText,
                    style = MaterialTheme.typography.displaySmall,
                    color = when (visualState) {
                        ChronometerVisualState.RUNNING -> colors.primaryText
                        ChronometerVisualState.SUSPENDED -> colors.bad
                        else -> scheme.onSurface
                    },
                    maxLines = 1,
                    modifier = Modifier.bone(loading, widthFraction = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun ChronometerPreviewBody() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AtelierChronometer(
            elapsedText = "01:24:18",
            statusLabel = "En curso",
            visualState = ChronometerVisualState.RUNNING,
            caption = "Est. 2.0 h"
        )
        AtelierChronometer(
            elapsedText = "00:42:05",
            statusLabel = "En pausa",
            visualState = ChronometerVisualState.PAUSED,
            caption = "Refrigerio"
        )
    }
}

@Preview(name = "Cronómetro · claro", widthDp = 390, showBackground = true)
@Composable
private fun ChronometerPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { ChronometerPreviewBody() }

@Preview(
    name = "Cronómetro · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ChronometerPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { ChronometerPreviewBody() }
