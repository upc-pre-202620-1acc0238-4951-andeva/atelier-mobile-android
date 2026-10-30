package com.andeva.atelier.mobile.features.workorders.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes

/** Detalle de una orden con sus tareas. [onOpenTask] recibe el id de la orden y el de la tarea (US33). */
@Composable
fun WorkOrderDetailScreen(
    onBack: () -> Unit,
    onOpenTask: (workOrderId: String, taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkOrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkOrderDetailContent(state, onBack, viewModel::load, onOpenTask, modifier)
}

@Composable
internal fun WorkOrderDetailContent(
    state: WorkOrderDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenTask: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.order_detail_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val detail = state.detail
            if (detail != null) {
                val order = detail.workOrder
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind())
                }
                Text(
                    text = order.internalNumber?.let { stringResource(R.string.task_card_work_order, it) }
                        ?: stringResource(R.string.orders_no_number),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                order.plate?.let {
                    Text(
                        text = stringResource(R.string.task_card_plate, it),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
                Text(
                    text = stringResource(R.string.order_tasks_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (detail.tasks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.order_tasks_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
                detail.tasks.forEach { task -> TaskRow(task, onClick = { onOpenTask(detail.workOrder.id, task.id.value) }) }
            } else if (state.isLoading) {
                Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            if (detail == null && !state.isLoading) {
                AtelierButton(
                    text = stringResource(R.string.branch_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskDetail, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierChip(
                text = stringResource(taskStatusLabelRes(task.status, task.holdReason)),
                kind = taskStatusChipKind(task.status, task.holdReason)
            )
            if (task.hasPendingSuspension) {
                AtelierChip(text = stringResource(R.string.task_suspension_pending), kind = ChipKind.Wait)
            }
        }
        task.serviceName?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = task.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        task.mechanicName?.let {
            Text(
                text = stringResource(R.string.order_task_mechanic, it),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        Text(
            text = stringResource(R.string.order_task_parts, task.products.size),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
    }
}

@Preview(name = "Orden · tareas · oscuro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WorkOrderDetailPreview() = AtelierWorkshopTheme(darkTheme = true) {
    WorkOrderDetailContent(
        state = WorkOrderDetailUiState(isLoading = false, error = null, detail = null),
        onBack = {}, onRetry = {}, onOpenTask = { _, _ -> }
    )
}
