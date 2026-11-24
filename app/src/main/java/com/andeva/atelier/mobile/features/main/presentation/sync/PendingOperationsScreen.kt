package com.andeva.atelier.mobile.features.main.presentation.sync

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.main.domain.PendingOperation
import com.andeva.atelier.mobile.features.main.domain.PendingOperationStatus
import java.time.Instant
import java.time.format.DateTimeFormatter

private val SyncTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(BusinessTime.ZONE)

/**
 * Cola de sincronización: lo que se guardó en el teléfono y aún no llegó al servidor (asistencia, fotos, tareas,
 * propuestas y telemetría/fallas OBD-II, docs/06-offline-sync.md). Tocar una fila abre su detalle con reintentar o descartar.
 */
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
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = state.operations.firstOrNull { it.id == selectedId }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.sync_queue_title), onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "summary") {
                val colors = AtelierTheme.colors
                AtelierSurface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    tier = SurfaceTier.Elevated,
                    shape = CardLargeShape,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.syncingCount > 0 || state.isSyncingNow) {
                                AtelierLiveBeacon(color = colors.info, size = 8.dp)
                            }
                            Text(
                                text = stringResource(R.string.sync_summary_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (state.hasLoaded && state.operations.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.sync_summary_counts,
                                    state.pendingCount,
                                    state.syncingCount,
                                    state.failedCount
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primaryText
                            )
                        }
                        Text(
                            text = stringResource(R.string.sync_queue_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkSecondary
                        )
                        if (!state.isOnline) {
                            AtelierBanner(
                                message = stringResource(R.string.sync_status_offline),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Wait
                            )
                        }
                        if (state.isSyncingNow) {
                            state.notice?.let {
                                AtelierBanner(message = it.asString(), icon = Icons.Outlined.Sync, kind = BannerKind.Info)
                            }
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
                            if (!state.isOnline) AtelierRequiresConnection()
                        }
                    }
                }
            }
            when {
                !state.hasLoaded -> items(3, key = { "bone-$it" }) {
                    AtelierListRow(title = "Marcación de ingreso", subtitle = "Creado 16/11 08:02", loading = true)
                }
                state.operations.isEmpty() -> item(key = "empty") {
                    AtelierEmptyState(
                        text = stringResource(R.string.sync_empty_line),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                else -> items(state.operations, key = { it.id }) { op ->
                    AtelierListRow(
                        title = operationTypeLabel(op.type),
                        subtitle = op.rowSubtitle(),
                        onClick = { selectedId = op.id },
                        trailing = { OperationChip(op.status) }
                    )
                }
            }
        }
    }

    selected?.let { op ->
        OperationSheet(
            operation = op,
            isOnline = state.isOnline,
            onDismiss = { selectedId = null },
            onRetry = {
                onRetry(op.id)
                selectedId = null
            },
            onDiscard = {
                onDiscard(op.id)
                selectedId = null
            }
        )
    }
}

@Composable
private fun OperationChip(status: PendingOperationStatus) {
    when (status) {
        PendingOperationStatus.PENDING -> AtelierChip(stringResource(R.string.sync_op_pending), kind = ChipKind.Idle)
        PendingOperationStatus.SYNCING -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierLiveBeacon(color = AtelierTheme.colors.info, size = 8.dp)
            AtelierChip(stringResource(R.string.sync_op_syncing), kind = ChipKind.Info)
        }
        PendingOperationStatus.FAILED -> AtelierChip(stringResource(R.string.sync_op_failed), kind = ChipKind.Bad)
    }
}

/** Lo que hay que saber de un vistazo: cuándo se guardó y cuántas veces se intentó. */
@Composable
private fun PendingOperation.rowSubtitle(): String =
    stringResource(R.string.sync_op_meta, SyncTimeFormat.format(createdAt), attempts)

/**
 * Detalle de una operación. Reintentar solo existe si falló; descartar pierde el registro del teléfono y por
 * eso vive aquí, no en la fila. Ambas son acciones sobre la cola local: reintentar necesita red para llegar.
 */
@Composable
private fun OperationSheet(
    operation: PendingOperation,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit
) {
    AtelierBottomSheet(
        title = operationTypeLabel(operation.type),
        onDismiss = onDismiss,
        actions = {
            if (operation.status == PendingOperationStatus.FAILED) {
                if (!isOnline) AtelierRequiresConnection()
                AtelierButton(
                    text = stringResource(R.string.sync_retry_btn),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isOnline,
                    icon = Icons.Outlined.Sync
                )
            }
            if (operation.status != PendingOperationStatus.SYNCING) {
                AtelierButton(
                    text = stringResource(R.string.sync_discard_btn),
                    onClick = onDiscard,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Destructive
                )
            }
        }
    ) {
        OperationChip(operation.status)
        Text(
            text = operation.rowSubtitle(),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        operation.lastErrorMessage?.takeIf { it.isNotBlank() }?.let {
            AtelierBanner(message = it, icon = Icons.Outlined.ErrorOutline)
        }
        if (operation.status != PendingOperationStatus.SYNCING) {
            Text(
                text = stringResource(R.string.sync_discard_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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
    "SUBMIT_PROPOSAL" -> stringResource(R.string.sync_type_submit_proposal)
    "TELEMETRY_BATCH" -> stringResource(R.string.sync_type_telemetry)
    "REGISTER_FAULT" -> stringResource(R.string.sync_type_register_fault)
    "RESOLVE_FAULT" -> stringResource(R.string.sync_type_resolve_fault)
    else -> type
}

// region Previews

private fun previewOp(id: String, type: String, status: PendingOperationStatus, attempts: Int, error: String? = null) =
    PendingOperation(
        id = id, type = type, status = status, attempts = attempts, lastErrorMessage = error,
        createdAt = Instant.parse("2026-11-16T13:02:00Z"), dependsOn = null
    )

private val previewState = PendingOperationsUiState(
    operations = listOf(
        previewOp("1", "ATTACH_INTAKE_IMAGE", PendingOperationStatus.FAILED, 3, "La foto ya no está en el teléfono"),
        previewOp("2", "ATTACH_INTAKE_IMAGE", PendingOperationStatus.PENDING, 0),
        previewOp("3", "UPLOAD_FILE", PendingOperationStatus.SYNCING, 1)
    ),
    hasLoaded = true,
    isOnline = false
)

@Preview(name = "Cola · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PendingOperationsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    PendingOperationsContent(previewState, {}, {}, {}, {})
}

@Preview(name = "Cola · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PendingOperationsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PendingOperationsContent(previewState.copy(isOnline = true), {}, {}, {}, {})
}

@Preview(name = "Cola · vacía", widthDp = 390, heightDp = 844)
@Composable
private fun PendingOperationsPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PendingOperationsContent(PendingOperationsUiState(hasLoaded = true), {}, {}, {}, {})
}

// endregion
