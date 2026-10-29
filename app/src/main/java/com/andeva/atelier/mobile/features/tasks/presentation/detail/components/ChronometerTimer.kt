package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** Segundos de labor efectiva: desde `startedAt` menos lo pausado; nunca negativo (US27). */
internal fun effectiveSeconds(startedAt: Instant, totalPausedSeconds: Long, now: Instant): Long =
    (Duration.between(startedAt, now).seconds - totalPausedSeconds).coerceAtLeast(0)

/** `HH:MM:SS` con horas sin tope. */
internal fun formatElapsed(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

/**
 * Cronómetro en vivo de la labor efectiva (US27 E1). Cuenta cada segundo mientras [running]; en pausa muestra el
 * tiempo acumulado hasta el momento de pausar y el total de pausas.
 */
@Composable
internal fun ChronometerTimer(
    startedAt: Instant?,
    totalPausedSeconds: Long,
    running: Boolean,
    modifier: Modifier = Modifier,
    nowProvider: () -> Instant = Instant::now
) {
    var now by remember { mutableStateOf(nowProvider()) }
    LaunchedEffect(running) {
        while (running) {
            now = nowProvider()
            delay(1000)
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.task_timer_label),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = startedAt?.let { formatElapsed(effectiveSeconds(it, totalPausedSeconds, now)) } ?: formatElapsed(0),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (totalPausedSeconds > 0) {
            Text(
                text = stringResource(R.string.task_timer_paused_total, formatElapsed(totalPausedSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}
