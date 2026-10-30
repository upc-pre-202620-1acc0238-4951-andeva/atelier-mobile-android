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
import com.andeva.atelier.mobile.features.tasks.domain.LaborTime
import kotlinx.coroutines.delay
import java.time.Instant

/** Segundos de labor efectiva: desde `startedAt` menos lo pausado; nunca negativo (US27). */
internal fun effectiveSeconds(startedAt: Instant, totalPausedSeconds: Long, now: Instant): Long =
    LaborTime.effectiveSeconds(startedAt, totalPausedSeconds, now)

/** `HH:MM:SS` con horas sin tope. */
internal fun formatElapsed(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

/** Texto del cronómetro y si el tiempo de una tarea pausada se desconoce (D1). */
internal data class TimerDisplay(val text: String, val unknown: Boolean)

/**
 * Qué mostrar según el estado de la tarea. En marcha, cuenta desde `startedAt`. En pausa, el valor guardado al pausar
 * ([pausedElapsedSeconds]); sin él (la pausa se hizo en otro dispositivo) no se inventa un número: el backend no envía
 * la hora de la pausa y `now − startedAt` incluiría el tiempo pausado (D1).
 */
internal fun timerDisplay(
    startedAt: Instant?,
    totalPausedSeconds: Long,
    running: Boolean,
    paused: Boolean,
    pausedElapsedSeconds: Long?,
    now: Instant
): TimerDisplay = when {
    running && startedAt != null ->
        TimerDisplay(formatElapsed(effectiveSeconds(startedAt, totalPausedSeconds, now)), unknown = false)
    paused && pausedElapsedSeconds != null -> TimerDisplay(formatElapsed(pausedElapsedSeconds), unknown = false)
    paused -> TimerDisplay(UNKNOWN_ELAPSED, unknown = true)
    else -> TimerDisplay(formatElapsed(0), unknown = false)
}

internal const val UNKNOWN_ELAPSED = "--:--:--"

/**
 * Cronómetro de la labor efectiva (US27 E1). Cuenta cada segundo mientras [running]; en pausa muestra el tiempo
 * guardado al pausar y el total de pausas.
 */
@Composable
internal fun ChronometerTimer(
    startedAt: Instant?,
    totalPausedSeconds: Long,
    running: Boolean,
    paused: Boolean,
    pausedElapsedSeconds: Long?,
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
    val display = timerDisplay(startedAt, totalPausedSeconds, running, paused, pausedElapsedSeconds, now)
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
            text = display.text,
            style = MaterialTheme.typography.displayMedium,
            color = if (display.unknown) AtelierTheme.colors.muted else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (display.unknown) {
            Text(
                text = stringResource(R.string.task_timer_unknown_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (totalPausedSeconds > 0) {
            Text(
                text = stringResource(R.string.task_timer_paused_total, formatElapsed(totalPausedSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}
