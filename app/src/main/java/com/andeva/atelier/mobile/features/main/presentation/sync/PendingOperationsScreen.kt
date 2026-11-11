package com.andeva.atelier.mobile.features.main.presentation.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.main.domain.PendingOperation
import com.andeva.atelier.mobile.features.main.domain.PendingOperationStatus
import java.time.format.DateTimeFormatter

private val SyncTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(BusinessTime.ZONE)

@Composable
fun PendingOperationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PendingOperationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PendingOperationsContent(
        state = state,
        onBack = onBack,
        onSyncNow = viewModel::onSyncNow,
        onRetry = viewModel::onRetry,
        onDiscard = viewModel::onDiscard,
        modifier = modifier
    )
}

@Composable
internal fun PendingOperationsContent(
    state: PendingOperationsUiState,
    onBack: () -> Unit,
    onSyncNow: () -> Unit,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.sync_queue_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_queue_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )

            AtelierBanner(
                message = stringResource(
                    if (state.isOnline) R.string.sync_status_online else R.string.sync_status_offline
                ),
                icon = if (state.isOnline) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                kind = if (state.isOnline) BannerKind.Ok else BannerKind.Wait
            )

            state.notice?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.Sync, kind = BannerKind.Info)
            }

            if (state.operations.isNotEmpty()) {
                AtelierButton(
                    text = stringResource(R.string.sync_now_btn, state.operations.size),
                    onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isOnline,
                    loading = state.isSyncingNow,
                    icon = Icons.Outlined.Sync
                )
            }

            if (state.operations.isEmpty()) {
                AtelierBanner(
                    title = stringResource(R.string.sync_empty_title),
                    message = stringResource(R.string.sync_empty_body),
                    icon = Icons.Outlined.CheckCircle,
                    kind = BannerKind.Ok
                )
            } else {
                state.operations.forEach { op ->
                    PendingOperationCard(
                        operation = op,
                        onRetry = { onRetry(op.id) },
                        onDiscard = { onDiscard(op.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingOperationCard(
    operation: PendingOperation,
    onRetry: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = operationTypeLabel(operation.type),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            AtelierChip(
                text = when (operation.status) {
                    PendingOperationStatus.PENDING -> stringResource(R.string.sync_op_pending)
                    PendingOperationStatus.SYNCING -> stringResource(R.string.sync_op_syncing)
                    PendingOperationStatus.FAILED -> stringResource(R.string.sync_op_failed)
                },
                kind = when (operation.status) {
                    PendingOperationStatus.PENDING -> ChipKind.Wait
                    PendingOperationStatus.SYNCING -> ChipKind.Info
                    PendingOperationStatus.FAILED -> ChipKind.Bad
                }
            )
        }
        Text(
            text = stringResource(
                R.string.sync_op_meta,
                SyncTimeFormat.format(operation.createdAt),
                operation.attempts
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AtelierTheme.colors.muted
        )
        operation.lastErrorMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.bad
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (operation.status == PendingOperationStatus.FAILED) {
                AtelierButton(
                    text = stringResource(R.string.sync_retry_btn),
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    style = AtelierButtonStyle.Tonal
                )
            }
            if (operation.status != PendingOperationStatus.SYNCING) {
                AtelierButton(
                    text = stringResource(R.string.sync_discard_btn),
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    style = AtelierButtonStyle.Outlined
                )
            }
        }
    }
}

@Composable
private fun operationTypeLabel(type: String): String = when (type) {
    "CLOCK_IN" -> stringResource(R.string.sync_type_clock_in)
    "CLOCK_OUT" -> stringResource(R.string.sync_type_clock_out)
    "SUBMIT_JUSTIFICATION" -> stringResource(R.string.sync_type_justification)
    "UPLOAD_FILE" -> stringResource(R.string.sync_type_upload_file)
    "ATTACH_INTAKE_IMAGE" -> stringResource(R.string.sync_type_intake_image)
    "ATTACH_TASK_EVIDENCE" -> stringResource(R.string.sync_type_task_evidence)
    "START_TASK" -> stringResource(R.string.sync_type_start_task)
    "PAUSE_TASK" -> stringResource(R.string.sync_type_pause_task)
    "RESUME_TASK" -> stringResource(R.string.sync_type_resume_task)
    "COMPLETE_TASK" -> stringResource(R.string.sync_type_complete_task)
    "TELEMETRY_BATCH" -> stringResource(R.string.sync_type_telemetry)
    "REGISTER_FAULT" -> stringResource(R.string.sync_type_register_fault)
    "RESOLVE_FAULT" -> stringResource(R.string.sync_type_resolve_fault)
    else -> type
}
