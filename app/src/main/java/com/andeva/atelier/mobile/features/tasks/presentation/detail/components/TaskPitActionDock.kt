package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierAdaptiveRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.detail.TaskAction
import com.andeva.atelier.mobile.features.tasks.presentation.detail.TaskDetailActions
import com.andeva.atelier.mobile.features.tasks.presentation.detail.TaskDetailUiState

/**
 * Dock de acción elevado en el tercio inferior para los controles primarios de fosa
 * (Iniciar / Reanudar, Pausar, Cerrar tarea), con baliza en vivo cuando la labor está corriendo
 * y objetivos táctiles ≥ 56 dp.
 */
@Composable
internal fun TaskPitActionDock(
    state: TaskDetailUiState,
    actions: TaskDetailActions
) {
    val showStart = state.canTrackTime && (state.status == TaskStatus.ASSIGNED || state.isPausedByMechanic)
    val showPause = state.canTrackTime && state.status == TaskStatus.IN_PROGRESS
    val showComplete = state.canComplete && state.status == TaskStatus.IN_PROGRESS

    if (!showStart && !showPause && !showComplete) return

    val haptics = rememberAtelierHaptics()
    val colors = AtelierTheme.colors
    val isRunning = state.status == TaskStatus.IN_PROGRESS
    val statusTextRes = when {
        isRunning -> R.string.pit_dock_running
        state.isPausedByMechanic -> R.string.pit_dock_paused
        else -> R.string.pit_dock_ready
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AtelierSurface(
            modifier = Modifier.fillMaxWidth(),
            tier = SurfaceTier.Floating,
            shape = CardLargeShape,
            contentPadding = PaddingValues(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRunning) {
                        AtelierLiveBeacon(color = colors.info, pulsing = true)
                    }
                    Text(
                        text = stringResource(statusTextRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRunning) colors.primaryText else colors.inkSecondary
                    )
                }
                if (!state.isOnline) {
                    AtelierRequiresConnection()
                }
                val startOrPauseContent: (@Composable (Modifier) -> Unit)? = when {
                    showStart -> { mod ->
                        AtelierButton(
                            text = stringResource(if (state.isPausedByMechanic) R.string.task_resume else R.string.task_start),
                            onClick = {
                                haptics.confirm()
                                actions.onStart()
                            },
                            modifier = mod.heightIn(min = 56.dp),
                            enabled = state.canStart && state.isOnline,
                            loading = state.busyAction == TaskAction.START,
                            icon = Icons.Outlined.PlayArrow
                        )
                    }
                    showPause -> { mod ->
                        AtelierButton(
                            text = stringResource(R.string.task_pause),
                            onClick = {
                                haptics.toggle()
                                actions.onPauseClick()
                            },
                            modifier = mod.heightIn(min = 56.dp),
                            style = AtelierButtonStyle.Tonal,
                            enabled = state.canPause && state.isOnline,
                            loading = state.busyAction == TaskAction.PAUSE,
                            icon = Icons.Outlined.Pause
                        )
                    }
                    else -> null
                }
                val completeContent: (@Composable (Modifier) -> Unit)? = if (showComplete) {
                    { mod ->
                        AtelierButton(
                            text = stringResource(R.string.task_complete),
                            onClick = {
                                haptics.confirm()
                                actions.onCompleteClick()
                            },
                            modifier = mod.heightIn(min = 56.dp),
                            style = AtelierButtonStyle.Filled,
                            enabled = state.canClose && state.isOnline,
                            icon = Icons.Outlined.CheckCircle
                        )
                    }
                } else null

                when {
                    startOrPauseContent != null && completeContent != null -> {
                        AtelierAdaptiveRow(
                            horizontalSpacing = 12.dp,
                            verticalSpacing = 8.dp,
                            equalWidthInRow = true
                        ) {
                            startOrPauseContent(Modifier)
                            completeContent(Modifier)
                        }
                    }
                    startOrPauseContent != null -> startOrPauseContent(Modifier.fillMaxWidth())
                    completeContent != null -> completeContent(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
