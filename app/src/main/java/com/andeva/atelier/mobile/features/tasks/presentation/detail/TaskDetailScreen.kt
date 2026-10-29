package com.andeva.atelier.mobile.features.tasks.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ChronometerTimer
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ChargePartDialog
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.CompleteTaskDialog
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.PartEditDialog
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.PauseReasonDialog
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.TaskPartsSection
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes
import java.math.BigDecimal
import java.time.Instant

/** US27: detalle de la tarea con cronómetro, pausa tipificada y cierre. */
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Al cerrar la tarea sale de Mis tareas: se vuelve a la bandeja.
    LaunchedEffect(state.completed) { if (state.completed) onBack() }
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
            onPartEditConfirm = viewModel::onPartEditConfirm
        ),
        modifier = modifier
    )
}

internal class TaskDetailActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onStart: () -> Unit,
    val onPauseClick: () -> Unit,
    val onPauseDismiss: () -> Unit,
    val onPause: (PauseReason) -> Unit,
    val onCompleteClick: () -> Unit,
    val onCompleteDismiss: () -> Unit,
    val onHoursChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onCompleteConfirm: () -> Unit,
    val onChargeClick: () -> Unit,
    val onChargeDismiss: () -> Unit,
    val onChargeQueryChange: (String) -> Unit,
    val onPartSelect: (PartOption) -> Unit,
    val onChargeQuantityChange: (String) -> Unit,
    val onChargeConfirm: () -> Unit,
    val onEditPartClick: (TaskProduct) -> Unit,
    val onReturnPartClick: (TaskProduct) -> Unit,
    val onPartEditDismiss: () -> Unit,
    val onPartQuantityChange: (String) -> Unit,
    val onPartEditConfirm: () -> Unit
)

@Composable
internal fun TaskDetailContent(
    state: TaskDetailUiState,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.task_detail_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.task?.let { task -> TaskHeader(task, state) }
            ChronometerTimer(
                startedAt = state.startedAt,
                totalPausedSeconds = state.totalPausedSeconds,
                running = state.status == TaskStatus.IN_PROGRESS
            )
            state.notice?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
            }
            if (state.completeForm == null && state.chargeForm == null && state.partEdit == null) {
                state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }
            if (state.isSuspended) {
                AtelierBanner(
                    message = stringResource(R.string.task_suspended_notice),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            if (!state.canTrackTime && !state.canComplete) {
                AtelierBanner(
                    message = stringResource(R.string.reception_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            TaskControls(state, actions)
            if (state.detail != null || state.canChargeParts) {
                TaskPartsSection(
                    products = state.products,
                    canCharge = state.canOpenCharge,
                    canEdit = state.canEditParts,
                    isBusy = state.isBusy,
                    onCharge = actions.onChargeClick,
                    onEdit = actions.onEditPartClick,
                    onReturn = actions.onReturnPartClick
                )
            }
            if (state.task == null && state.error != null) {
                AtelierButton(
                    text = stringResource(R.string.branch_retry),
                    onClick = actions.onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text
                )
            }
        }
    }

    if (state.pauseForm != null) {
        PauseReasonDialog(onSelect = actions.onPause, onDismiss = actions.onPauseDismiss)
    }
    state.chargeForm?.let { form ->
        ChargePartDialog(
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
        PartEditDialog(
            form = edit,
            isSaving = state.busyAction == TaskAction.EDIT_PART || state.busyAction == TaskAction.REMOVE_PART,
            invalidField = state.invalidField,
            error = state.error,
            onQuantityChange = actions.onPartQuantityChange,
            onConfirm = actions.onPartEditConfirm,
            onDismiss = actions.onPartEditDismiss
        )
    }
    state.completeForm?.let { form ->
        CompleteTaskDialog(
            form = form,
            isSaving = state.busyAction == TaskAction.COMPLETE,
            invalidField = state.invalidField,
            error = state.error,
            onHoursChange = actions.onHoursChange,
            onNotesChange = actions.onNotesChange,
            onConfirm = actions.onCompleteConfirm,
            onDismiss = actions.onCompleteDismiss
        )
    }
}

@Composable
private fun TaskHeader(task: Task, state: TaskDetailUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AtelierChip(
            text = stringResource(taskStatusLabelRes(state.status, state.holdReason)),
            kind = taskStatusChipKind(state.status, state.holdReason)
        )
        Text(
            text = task.plate?.let { stringResource(R.string.task_card_plate, it) }
                ?: stringResource(R.string.task_card_no_plate),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        state.detail?.serviceName?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = state.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        task.bayName?.let {
            Text(
                text = stringResource(R.string.task_card_bay, it),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

@Composable
private fun TaskControls(state: TaskDetailUiState, actions: TaskDetailActions) {
    if (state.canTrackTime && (state.status == TaskStatus.ASSIGNED || state.isPausedByMechanic)) {
        AtelierButton(
            text = stringResource(if (state.isPausedByMechanic) R.string.task_resume else R.string.task_start),
            onClick = actions.onStart,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canStart,
            loading = state.busyAction == TaskAction.START
        )
    }
    if (state.canTrackTime && state.status == TaskStatus.IN_PROGRESS) {
        AtelierButton(
            text = stringResource(R.string.task_pause),
            onClick = actions.onPauseClick,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal,
            enabled = state.canPause,
            loading = state.busyAction == TaskAction.PAUSE
        )
    }
    if (state.canComplete && state.status == TaskStatus.IN_PROGRESS) {
        AtelierButton(
            text = stringResource(R.string.task_complete),
            onClick = actions.onCompleteClick,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Outlined,
            enabled = state.canClose
        )
    }
}

private fun previewTask(status: TaskStatus, hold: HoldReason? = null) = Task(
    id = TaskId("t1"), workOrderId = "wo1", workOrderNumber = 1042, workOrderStatus = "IN_PROGRESS",
    vehicleId = "v1", plate = "ABC-123", branchId = "b1", bayId = "bay1", bayName = "B-2", serviceName = null,
    mechanicId = "m1", status = status, description = "Cambio de pastillas de freno delanteras",
    estimatedHours = BigDecimal("1.5"), holdReason = hold, missingItemDescription = null,
    pendingSuspensionReason = null, startedAt = Instant.now().minusSeconds(3725), totalPausedSeconds = 300
)

private val previewActions = TaskDetailActions(
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
)

@Preview(name = "Tarea · en curso · oscuro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun TaskDetailPreviewRunning() = AtelierWorkshopTheme(darkTheme = true) {
    TaskDetailContent(
        state = TaskDetailUiState(
            task = previewTask(TaskStatus.IN_PROGRESS), isLoading = false, canTrackTime = true, canComplete = true
        ),
        actions = previewActions
    )
}

@Preview(name = "Tarea · en pausa · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun TaskDetailPreviewPaused() = AtelierWorkshopTheme(darkTheme = false) {
    TaskDetailContent(
        state = TaskDetailUiState(
            task = previewTask(TaskStatus.ON_HOLD, HoldReason.BREAK), isLoading = false,
            canTrackTime = true, canComplete = true
        ),
        actions = previewActions
    )
}
