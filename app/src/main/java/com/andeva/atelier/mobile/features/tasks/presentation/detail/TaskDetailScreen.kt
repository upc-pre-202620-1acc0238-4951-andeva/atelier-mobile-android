package com.andeva.atelier.mobile.features.tasks.presentation.detail

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierDecisionRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalAtelierMinTouchTarget
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.tasks.domain.EvidenceKind
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.PendingEvidence
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskEvidence
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ChargePartSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ChronometerTimer
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.CompleteTaskSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.FailedEvidenceSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.PartEditSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.PauseReasonSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ReopenTaskSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.RequestSuspensionSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ReviewSuspensionSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.TaskEvidenceSection
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.TaskPartsSection
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.TaskPitActionDock
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.UnsavedEvidenceExitSheet
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.rememberTaskPhotoCapture
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes
import java.math.BigDecimal
import java.time.Instant

/**
 * US27–US30, US23, US33: detalle de tarea con panel Ficha, cronómetro tabular, controles grandes de fosa,
 * evidencias offline de desmontaje/montaje, suspensiones mecánico/supervisor, reapertura e imputación FIFO.
 */
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReportFinding: ((workOrderId: String) -> Unit)? = null,
    onOpenObd: (vehicleId: String, plate: String?) -> Unit = { _, _ -> },
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()
    val openCamera = rememberTaskPhotoCapture(
        onCaptured = viewModel::onEvidenceCaptured,
        onDenied = viewModel::onCameraDenied
    )

    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }
    // Al cerrar la tarea sale de Mis tareas: confirma con háptica y vuelve a la bandeja.
    LaunchedEffect(state.completed) {
        if (state.completed) {
            haptics.confirm()
            onBack()
        }
    }

    TaskDetailContent(
        state = state,
        actions = TaskDetailActions(
            onBack = onBack,
            onRetry = viewModel::load,
            onStart = viewModel::onStart,
            onPauseClick = viewModel::onPauseClick,
            onPauseDismiss = viewModel::onPauseDismiss,
            onPause = viewModel::onPause,
            onCompleteClick = viewModel::onCompleteClick,
            onCompleteDismiss = viewModel::onCompleteDismiss,
            onHoursChange = viewModel::onHoursChange,
            onNotesChange = viewModel::onNotesChange,
            onCompleteConfirm = viewModel::onCompleteConfirm,
            onReopenClick = viewModel::onReopenClick,
            onReopenDismiss = viewModel::onReopenDismiss,
            onReopenReasonChange = viewModel::onReopenReasonChange,
            onReopenConfirm = viewModel::onReopenConfirm,
            onEvidenceKindChange = viewModel::onEvidenceKindChange,
            onEvidenceNoteChange = viewModel::onEvidenceNoteChange,
            onTakeEvidencePhoto = openCamera,
            onSelectFailedEvidence = viewModel::onSelectFailedEvidence,
            onDismissFailedEvidence = viewModel::onDismissFailedEvidence,
            onRetryFailedEvidence = viewModel::onRetryFailedEvidence,
            onDiscardFailedEvidence = viewModel::onDiscardFailedEvidence,
            onRequestSuspensionClick = viewModel::onRequestSuspensionClick,
            onRequestSuspensionDismiss = viewModel::onRequestSuspensionDismiss,
            onSuspensionMissingItemChange = viewModel::onSuspensionMissingItemChange,
            onSuspensionPartQueryChange = viewModel::onSuspensionPartQueryChange,
            onSuspensionPartSelect = viewModel::onSuspensionPartSelect,
            onRequestSuspensionConfirm = viewModel::onRequestSuspensionConfirm,
            onReviewSuspensionClick = viewModel::onReviewSuspensionClick,
            onReviewSuspensionDismiss = viewModel::onReviewSuspensionDismiss,
            onApproveSuspension = viewModel::onApproveSuspension,
            onRejectSuspension = viewModel::onRejectSuspension,
            onResumeSuspendedTask = viewModel::onResumeSuspendedTask,
            onChargeClick = viewModel::onChargeClick,
            onChargeDismiss = viewModel::onChargeDismiss,
            onChargeQueryChange = viewModel::onChargeQueryChange,
            onPartSelect = viewModel::onPartSelect,
            onChargeQuantityChange = viewModel::onChargeQuantityChange,
            onChargeConfirm = viewModel::onChargeConfirm,
            onEditPartClick = viewModel::onEditPartClick,
            onReturnPartClick = viewModel::onReturnPartClick,
            onPartEditDismiss = viewModel::onPartEditDismiss,
            onPartQuantityChange = viewModel::onPartQuantityChange,
            onPartEditConfirm = viewModel::onPartEditConfirm,
            onReportFinding = onReportFinding,
            onOpenObd = onOpenObd
        ),
        modifier = modifier
    )
}

internal class TaskDetailActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onPauseClick: () -> Unit = {},
    val onPauseDismiss: () -> Unit = {},
    val onPause: (PauseReason) -> Unit = {},
    val onCompleteClick: () -> Unit = {},
    val onCompleteDismiss: () -> Unit = {},
    val onHoursChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onCompleteConfirm: () -> Unit = {},
    val onReopenClick: () -> Unit = {},
    val onReopenDismiss: () -> Unit = {},
    val onReopenReasonChange: (String) -> Unit = {},
    val onReopenConfirm: () -> Unit = {},
    val onEvidenceKindChange: (EvidenceKind) -> Unit = {},
    val onEvidenceNoteChange: (String) -> Unit = {},
    val onTakeEvidencePhoto: () -> Unit = {},
    val onSelectFailedEvidence: (PendingEvidence) -> Unit = {},
    val onDismissFailedEvidence: () -> Unit = {},
    val onRetryFailedEvidence: () -> Unit = {},
    val onDiscardFailedEvidence: () -> Unit = {},
    val onRequestSuspensionClick: () -> Unit = {},
    val onRequestSuspensionDismiss: () -> Unit = {},
    val onSuspensionMissingItemChange: (String) -> Unit = {},
    val onSuspensionPartQueryChange: (String) -> Unit = {},
    val onSuspensionPartSelect: (PartOption?) -> Unit = {},
    val onRequestSuspensionConfirm: () -> Unit = {},
    val onReviewSuspensionClick: () -> Unit = {},
    val onReviewSuspensionDismiss: () -> Unit = {},
    val onApproveSuspension: () -> Unit = {},
    val onRejectSuspension: () -> Unit = {},
    val onResumeSuspendedTask: () -> Unit = {},
    val onChargeClick: () -> Unit = {},
    val onChargeDismiss: () -> Unit = {},
    val onChargeQueryChange: (String) -> Unit = {},
    val onPartSelect: (PartOption) -> Unit = {},
    val onChargeQuantityChange: (String) -> Unit = {},
    val onChargeConfirm: () -> Unit = {},
    val onEditPartClick: (TaskProduct) -> Unit = {},
    val onReturnPartClick: (TaskProduct) -> Unit = {},
    val onPartEditDismiss: () -> Unit = {},
    val onPartQuantityChange: (String) -> Unit = {},
    val onPartEditConfirm: () -> Unit = {},
    val onReportFinding: ((workOrderId: String) -> Unit)? = null,
    val onOpenObd: (vehicleId: String, plate: String?) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDetailContent(
    state: TaskDetailUiState,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier
) = CompositionLocalProvider(LocalAtelierMinTouchTarget provides 56.dp) {
    val hasData = state.task != null || state.detail != null
    val showSkeleton = rememberDelayedLoading(state.isLoading && !hasData)
    var confirmExitWithUnsavedNote by remember { mutableStateOf(false) }

    BackHandler(enabled = state.hasUnsavedChanges && !state.hasOpenSheet && !confirmExitWithUnsavedNote) {
        confirmExitWithUnsavedNote = true
    }

    val handleBack = {
        if (state.hasUnsavedChanges && !state.hasOpenSheet) {
            confirmExitWithUnsavedNote = true
        } else {
            actions.onBack()
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TaskPitActionDock(state = state, actions = actions)
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && hasData,
            onRefresh = actions.onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "ficha-panel") {
                    TaskFichaHeader(state = state, loading = showSkeleton, onBack = handleBack)
                }
                item(key = "refresh-bar") {
                    AtelierRefreshBar(visible = state.isLoading && hasData)
                }

                if (!hasData && !showSkeleton && !state.isLoading && state.error != null) {
                    item(key = "full-error") {
                        if (state.isOnline) {
                            AtelierErrorState(
                                message = state.error.asString(),
                                onRetry = actions.onRetry,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            AtelierOfflineState(
                                onRetry = actions.onRetry,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    return@LazyColumn
                }

                item(key = "banners") {
                    TaskStatusBanners(state = state, modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (state.canReviewSuspension) {
                    item(key = "supervisor-decision") {
                        SupervisorSuspensionBlock(
                            state = state,
                            onClick = actions.onReviewSuspensionClick,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (state.description.isNotBlank() || showSkeleton) {
                    item(key = "description") {
                        TaskDescriptionCard(
                            description = state.description,
                            loading = showSkeleton,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item(key = "chronometer") {
                    ChronometerTimer(
                        startedAt = state.startedAt,
                        totalPausedSeconds = state.totalPausedSeconds,
                        running = state.status == TaskStatus.IN_PROGRESS,
                        paused = state.status == TaskStatus.ON_HOLD,
                        pausedElapsedSeconds = state.pausedElapsedSeconds,
                        status = state.status,
                        holdReason = state.holdReason,
                        suspended = state.isSuspended,
                        estimatedHours = state.task?.estimatedHours,
                        loading = state.isLoading && !hasData,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item(key = "pit-controls") {
                    PitControlsSection(
                        state = state,
                        actions = actions,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                val showObd = state.canOpenObd && state.vehicleId != null
                val showFinding = state.canReportFinding && actions.onReportFinding != null &&
                    state.resolvedWorkOrderId.isNotBlank()
                if (showObd || showFinding) {
                    item(key = "quick-actions") {
                        QuickActionsSection(
                            state = state,
                            showObd = showObd,
                            showFinding = showFinding,
                            onOpenObd = actions.onOpenObd,
                            onReportFinding = actions.onReportFinding,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (state.canUploadPhotos || state.evidenceImages.isNotEmpty() || state.pendingEvidence.isNotEmpty()) {
                    item(key = "evidence-section") {
                        TaskEvidenceSection(
                            state = state,
                            onKindChange = actions.onEvidenceKindChange,
                            onNoteChange = actions.onEvidenceNoteChange,
                            onTakePhoto = actions.onTakeEvidencePhoto,
                            onSelectFailedEvidence = actions.onSelectFailedEvidence,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (state.detail != null || state.canChargeParts) {
                    item(key = "parts-section") {
                        TaskPartsSection(
                            products = state.products,
                            available = state.partsAvailable,
                            canCharge = state.canOpenCharge,
                            canEdit = state.canEditParts,
                            isOnline = state.isOnline,
                            isBusy = state.isBusy,
                            onCharge = actions.onChargeClick,
                            onEdit = actions.onEditPartClick,
                            onReturn = actions.onReturnPartClick,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (!state.partsAvailable && !state.isLoading) {
                    item(key = "retry-detail") {
                        AtelierButton(
                            text = stringResource(R.string.branch_retry),
                            onClick = actions.onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            style = AtelierButtonStyle.Text
                        )
                    }
                }
            }
        }
    }

    TaskDetailSheets(state = state, actions = actions)

    if (confirmExitWithUnsavedNote) {
        UnsavedEvidenceExitSheet(
            onConfirmExit = {
                confirmExitWithUnsavedNote = false
                actions.onBack()
            },
            onDismiss = { confirmExitWithUnsavedNote = false }
        )
    }
}

@Composable
private fun TaskFichaHeader(state: TaskDetailUiState, loading: Boolean, onBack: () -> Unit) {
    val task = state.task
    val detail = state.detail
    val title = when {
        loading -> "Cambio de pastillas"
        !detail?.serviceName.isNullOrBlank() -> detail!!.serviceName!!
        state.description.isNotBlank() -> state.description
        else -> stringResource(R.string.task_detail_title)
    }
    val subtitle = listOfNotNull(
        task?.bayName?.let { stringResource(R.string.task_card_bay, it) },
        task?.workOrderNumber?.let { stringResource(R.string.task_card_work_order, it) },
        detail?.mechanicName?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.order_task_mechanic, it) }
    ).joinToString(" · ").ifEmpty { null }

    AtelierFichaPanel(
        onBack = onBack,
        title = title,
        subtitle = subtitle,
        identity = state.plate?.let { plate -> { AtelierPlate(plate, size = PlateSize.L) } },
        status = state.status?.let { status ->
            {
                AtelierChip(
                    text = stringResource(taskStatusLabelRes(status, state.holdReason)),
                    kind = taskStatusChipKind(status, state.holdReason)
                )
            }
        }
    )
}

@Composable
private fun TaskStatusBanners(state: TaskDetailUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.notice?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
        }
        if (!state.hasOpenSheet) {
            state.error?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
            }
        }
        if (state.hasPendingSuspension && !state.canReviewSuspension) {
            AtelierBanner(
                title = stringResource(R.string.tasks_seg2_suspension_pending_title),
                message = state.pendingSuspensionReason?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.tasks_seg2_suspension_pending_body),
                icon = Icons.Outlined.HourglassEmpty,
                kind = BannerKind.Wait
            )
        }
        if (state.isSuspended && !state.canReviewSuspension) {
            AtelierBanner(
                message = stringResource(R.string.task_suspended_notice),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Wait
            )
        }
        if (!state.hasAnyActionPermission) {
            AtelierBanner(
                message = stringResource(R.string.reception_no_permission),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Wait
            )
        }
    }
}

@Composable
private fun SupervisorSuspensionBlock(
    state: TaskDetailUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSuspended = state.isSuspended
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AtelierSectionHeader(title = stringResource(R.string.orders_section_decisions), count = 1)
        AtelierDecisionRow(
            title = stringResource(
                if (isSuspended) R.string.tasks_seg2_suspension_resume_row_title else R.string.orders_suspension_title
            ),
            subtitle = if (isSuspended) {
                state.missingItemDescription?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.tasks_seg2_suspension_resume_row_hint)
            } else {
                state.pendingSuspensionReason?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.tasks_seg2_suspension_review_hint)
            },
            plate = state.plate,
            onClick = onClick
        )
    }
}

@Composable
private fun TaskDescriptionCard(
    description: String,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    AtelierGroupedCard(
        modifier = modifier,
        title = stringResource(R.string.tasks_seg2_description_section)
    ) {
        Text(
            text = description.ifBlank { "Descripción de la tarea técnica" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .bone(loading, widthFraction = 0.85f)
        )
    }
}

@Composable
private fun PitControlsSection(
    state: TaskDetailUiState,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAtelierHaptics()
    val showSuspend = state.canRequestHold && !state.hasPendingSuspension && !state.isSuspended &&
        (state.status == TaskStatus.IN_PROGRESS || state.status == TaskStatus.ASSIGNED || state.isPausedByMechanic)
    val showReopen = state.canReopenTask && state.status == TaskStatus.COMPLETED

    if (!showSuspend && !showReopen) return

    val hasDockAbove = (state.canTrackTime && (state.status == TaskStatus.ASSIGNED || state.isPausedByMechanic || state.status == TaskStatus.IN_PROGRESS)) ||
        (state.canComplete && state.status == TaskStatus.IN_PROGRESS)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AtelierSectionHeader(title = stringResource(R.string.tasks_seg2_controls_section))
        if (showSuspend) {
            AtelierButton(
                text = stringResource(R.string.tasks_seg2_suspension_request_btn),
                onClick = {
                    haptics.toggle()
                    actions.onRequestSuspensionClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                style = AtelierButtonStyle.Destructive,
                enabled = state.canRequestSuspension && state.isOnline,
                icon = Icons.Outlined.ReportProblem
            )
        }
        if (showReopen) {
            AtelierButton(
                text = stringResource(R.string.tasks_seg2_reopen_btn),
                onClick = {
                    haptics.toggle()
                    actions.onReopenClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                style = AtelierButtonStyle.Outlined,
                enabled = state.canReopen && state.isOnline,
                loading = state.busyAction == TaskAction.REOPEN,
                icon = Icons.Outlined.RestartAlt
            )
        }
        if (!state.isOnline && !hasDockAbove) AtelierRequiresConnection()
    }
}

@Composable
private fun QuickActionsSection(
    state: TaskDetailUiState,
    showObd: Boolean,
    showFinding: Boolean,
    onOpenObd: (vehicleId: String, plate: String?) -> Unit,
    onReportFinding: ((workOrderId: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AtelierSectionHeader(title = stringResource(R.string.tasks_seg2_quick_actions))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showObd && state.vehicleId != null) {
                AtelierButton(
                    text = stringResource(R.string.tasks_seg2_action_obd),
                    onClick = { onOpenObd(state.vehicleId!!, state.plate) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    style = AtelierButtonStyle.Tonal,
                    icon = bluetooth_searching
                )
            }
            if (showFinding && onReportFinding != null) {
                AtelierButton(
                    text = stringResource(R.string.tasks_seg2_action_finding),
                    onClick = { onReportFinding(state.resolvedWorkOrderId) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    style = AtelierButtonStyle.Outlined,
                    icon = Icons.Outlined.CameraAlt
                )
            }
        }
    }
}

@Composable
private fun TaskDetailSheets(state: TaskDetailUiState, actions: TaskDetailActions) {
    if (state.pauseForm != null) {
        PauseReasonSheet(
            isOnline = state.isOnline,
            isSaving = state.busyAction == TaskAction.PAUSE,
            onSelect = actions.onPause,
            onDismiss = actions.onPauseDismiss
        )
    }
    state.completeForm?.let { form ->
        CompleteTaskSheet(
            form = form,
            isOnline = state.isOnline,
            isSaving = state.busyAction == TaskAction.COMPLETE,
            invalidField = state.invalidField,
            error = state.error,
            onHoursChange = actions.onHoursChange,
            onNotesChange = actions.onNotesChange,
            onConfirm = actions.onCompleteConfirm,
            onDismiss = actions.onCompleteDismiss,
            hasAssemblyEvidence = !state.requiresAssemblyEvidence
        )
    }
    state.suspensionRequestForm?.let { form ->
        RequestSuspensionSheet(
            form = form,
            isOnline = state.isOnline,
            isSaving = state.busyAction == TaskAction.REQUEST_HOLD,
            invalidField = state.invalidField,
            error = state.error,
            onMissingItemChange = actions.onSuspensionMissingItemChange,
            onPartQueryChange = actions.onSuspensionPartQueryChange,
            onPartSelect = actions.onSuspensionPartSelect,
            onConfirm = actions.onRequestSuspensionConfirm,
            onDismiss = actions.onRequestSuspensionDismiss,
            canSearchParts = state.canReadPartsCatalog
        )
    }
    state.selectedFailedEvidence?.let { failed ->
        FailedEvidenceSheet(
            evidence = failed,
            onRetry = actions.onRetryFailedEvidence,
            onDiscard = actions.onDiscardFailedEvidence,
            onDismiss = actions.onDismissFailedEvidence
        )
    }
    if (state.reviewSuspensionForm != null) {
        val taskTitle = state.detail?.serviceName?.takeIf { it.isNotBlank() } ?: state.description
        ReviewSuspensionSheet(
            taskTitle = taskTitle,
            isSuspended = state.isSuspended,
            pendingReason = state.pendingSuspensionReason,
            missingItemDescription = state.missingItemDescription,
            requestedAt = state.pendingSuspensionRequestedAt,
            isOnline = state.isOnline,
            busyApprove = state.busyAction == TaskAction.APPROVE_HOLD,
            busyReject = state.busyAction == TaskAction.REJECT_HOLD,
            busyResume = state.busyAction == TaskAction.RESUME_HOLD,
            error = state.error,
            onApprove = actions.onApproveSuspension,
            onReject = actions.onRejectSuspension,
            onResume = actions.onResumeSuspendedTask,
            onDismiss = actions.onReviewSuspensionDismiss
        )
    }
    state.reopenForm?.let { form ->
        ReopenTaskSheet(
            form = form,
            isOnline = state.isOnline,
            isSaving = state.busyAction == TaskAction.REOPEN,
            error = state.error,
            onReasonChange = actions.onReopenReasonChange,
            onConfirm = actions.onReopenConfirm,
            onDismiss = actions.onReopenDismiss
        )
    }
    state.chargeForm?.let { form ->
        ChargePartSheet(
            form = form,
            isSaving = state.busyAction == TaskAction.CHARGE,
            invalidField = state.invalidField,
            error = state.error,
            onQueryChange = actions.onChargeQueryChange,
            onSelect = actions.onPartSelect,
            onQuantityChange = actions.onChargeQuantityChange,
            onConfirm = actions.onChargeConfirm,
            onDismiss = actions.onChargeDismiss
        )
    }
    state.partEdit?.let { edit ->
        PartEditSheet(
            form = edit,
            isSaving = state.busyAction == TaskAction.EDIT_PART || state.busyAction == TaskAction.REMOVE_PART,
            invalidField = state.invalidField,
            error = state.error,
            onQuantityChange = actions.onPartQuantityChange,
            onConfirm = actions.onPartEditConfirm,
            onDismiss = actions.onPartEditDismiss
        )
    }
}

// region Previews

private fun previewTask(
    status: TaskStatus,
    hold: HoldReason? = null,
    pendingSuspension: String? = null
) = Task(
    id = TaskId("t1"),
    workOrderId = "wo1",
    workOrderNumber = 1042,
    workOrderStatus = "IN_PROGRESS",
    vehicleId = "v1",
    plate = "BKL482",
    branchId = "b1",
    bayId = "bay1",
    bayName = "B-2",
    serviceName = "Cambio de pastillas y rectificado",
    mechanicId = "m1",
    status = status,
    description = "Cambio de pastillas delanteras, limpieza de mordazas y purga de líquido.",
    estimatedHours = BigDecimal("1.5"),
    holdReason = hold,
    missingItemDescription = if (hold == HoldReason.DEFECTIVE_PART) "Retén de mordaza roto" else null,
    pendingSuspensionReason = pendingSuspension,
    startedAt = Instant.now().minusSeconds(3725),
    totalPausedSeconds = 300
)

private val previewDetail = TaskDetail(
    id = TaskId("t1"),
    workOrderId = "wo1",
    serviceId = "s1",
    serviceName = "Cambio de pastillas y rectificado",
    mechanicId = "m1",
    mechanicName = "Andrés Vílchez",
    status = TaskStatus.IN_PROGRESS,
    description = "Cambio de pastillas delanteras, limpieza de mordazas y purga de líquido.",
    price = Money(BigDecimal("180.00"), Money.PEN),
    holdReason = null,
    missingItemDescription = null,
    totalPausedSeconds = 300,
    startedAt = Instant.now().minusSeconds(3725),
    completedAt = null,
    pendingSuspensionReason = null,
    pendingSuspensionRequestedAt = null,
    products = listOf(
        TaskProduct(
            id = "tp1",
            productId = "p1",
            productName = "Juego de pastillas Bosch",
            quantity = BigDecimal("1"),
            unitPrice = Money(BigDecimal("120.00"), Money.PEN),
            totalAmount = Money(BigDecimal("120.00"), Money.PEN)
        )
    ),
    evidenceImages = listOf(
        TaskEvidence("e1", "", "Desmontaje: pastilla desgastada", Instant.now())
    )
)

@Preview(name = "Detalle tarea · claro", widthDp = 390, heightDp = 960, showBackground = true)
@Composable
private fun TaskDetailPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    TaskDetailContent(
        state = TaskDetailUiState(
            task = previewTask(TaskStatus.IN_PROGRESS),
            detail = previewDetail,
            isLoading = false,
            canTrackTime = true,
            canComplete = true,
            canChargeParts = true,
            canUploadPhotos = true,
            canRequestHold = true,
            canReportFinding = true,
            canOpenObd = true,
            pendingEvidence = listOf(
                PendingEvidence("op1", null, "", "Montaje: discos nuevos", "PENDING", null, Instant.now())
            )
        ),
        actions = TaskDetailActions()
    )
}

@Preview(
    name = "Detalle tarea · supervisor · oscuro",
    widthDp = 390,
    heightDp = 960,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TaskDetailPreviewDarkSupervisor() = AtelierWorkshopTheme(darkTheme = true) {
    TaskDetailContent(
        state = TaskDetailUiState(
            task = previewTask(
                status = TaskStatus.ON_HOLD,
                hold = HoldReason.BREAK,
                pendingSuspension = "Falta retén de pistón izquierdo"
            ),
            detail = previewDetail.copy(
                status = TaskStatus.IN_PROGRESS,
                pendingSuspensionReason = "Falta retén de pistón izquierdo",
                missingItemDescription = "Retén de mordaza Brembo"
            ),
            isLoading = false,
            pausedElapsedSeconds = 2400L,
            canValidateHold = true,
            canEditParts = true,
            canChargeParts = true
        ),
        actions = TaskDetailActions()
    )
}

// endregion
