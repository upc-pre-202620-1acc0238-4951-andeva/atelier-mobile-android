package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes
import java.math.BigDecimal

/**
 * US26: bandeja de tareas asignadas en bahía.
 * Usa `AtelierMainTopBar` en raíz de pestaña o `AtelierTopBar` cuando se llega con pila atrás.
 */
@Composable
fun MyTasksScreen(
    onOpenTask: (workOrderId: String, taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    viewModel: MyTasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyTasksContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onFilterChange = viewModel::onFilterChange,
        onOpenTask = { onOpenTask(it.workOrderId, it.id.value) },
        onOpenNotifications = onOpenNotifications,
        onOpenSync = onOpenSync,
        onOpenProfile = onOpenProfile,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyTasksContent(
    state: MyTasksUiState,
    onBack: (() -> Unit)?,
    onRefresh: () -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton)
    val visible = state.visibleTasks

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val titleRes = if (state.isWorkshopScope) R.string.tasks_workshop_title else R.string.tasks_title
            if (onBack != null) {
                AtelierTopBar(title = stringResource(titleRes), onBack = onBack)
            } else {
                AtelierMainTopBar(
                    title = stringResource(titleRes),
                    onOpenNotifications = onOpenNotifications,
                    unreadNotificationsCount = state.unreadNotificationsCount,
                    onOpenSync = onOpenSync,
                    pendingSyncCount = state.pendingSyncCount,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.isLoaded && state.tasks.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .floatingBarPadding(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (!state.canRead) {
                    item(key = "no-permission") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.tasks_no_permission),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    return@LazyColumn
                }

                item(key = "summary-panel") {
                    TasksSummaryPanel(state = state, loading = showSkeleton, onFilterChange = onFilterChange)
                }

                item(key = "refresh-bar") {
                    AtelierRefreshBar(visible = state.isRefreshing && state.tasks.isNotEmpty())
                }

                item(key = "filter-chips") {
                    TaskFilterRow(state = state, onFilterChange = onFilterChange)
                }

                if (state.showingCache && state.tasks.isNotEmpty()) {
                    item(key = "cache-banner") {
                        AtelierBanner(
                            message = state.error?.asString() ?: stringResource(R.string.tasks_cache_notice),
                            icon = Icons.Outlined.WifiOff,
                            kind = BannerKind.Wait,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                when {
                    showSkeleton -> items(5, key = { "bone-$it" }) {
                        AtelierListRow(
                            title = "Cambio de pastillas de freno",
                            subtitle = "Bahía B-2 · OT #1042 · 1.5 h estimadas",
                            plate = "ABC123",
                            loading = true
                        )
                    }
                    state.showSkeleton -> Unit
                    state.tasks.isEmpty() && state.error != null && !state.isOnline -> item(key = "offline-state") {
                        AtelierOfflineState(onRetry = onRefresh, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    state.tasks.isEmpty() && state.error != null -> item(key = "error-state") {
                        AtelierErrorState(
                            message = state.error.asString(),
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.tasks.isEmpty() -> item(key = "empty-state") {
                        AtelierEmptyState(
                            text = stringResource(
                                if (state.isWorkshopScope) R.string.tasks_workshop_empty else R.string.tasks_empty
                            ),
                            actionLabel = stringResource(R.string.tasks_refresh),
                            onAction = onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    visible.isEmpty() -> item(key = "empty-filtered") {
                        AtelierEmptyState(
                            text = stringResource(R.string.tasks_seg2_empty_filtered),
                            actionLabel = stringResource(R.string.catalog_clear_filters),
                            onAction = { onFilterChange(TaskFilter.ALL) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(visible, key = { it.id.value }) { task ->
                        TaskRow(task = task, onClick = { onOpenTask(task) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksSummaryPanel(
    state: MyTasksUiState,
    loading: Boolean,
    onFilterChange: (TaskFilter) -> Unit
) {
    val count = state.tasks.size
    AtelierReportPanel(
        context = state.tenantName.ifBlank { stringResource(R.string.tasks_seg2_panel_context) },
        legend = if (loading) "4 tareas" else pluralStringResource(R.plurals.tasks_seg2_legend_count, count, count),
        caption = stringResource(
            if (state.isWorkshopScope) R.string.tasks_workshop_panel_caption else R.string.tasks_seg2_panel_caption
        ),
        figures = listOf(
            PanelFigure(
                value = state.pendingCount.toString(),
                label = stringResource(R.string.tasks_seg2_figure_pending),
                onClick = { onFilterChange(TaskFilter.ASSIGNED) }
            ),
            PanelFigure(
                value = state.inProgressCount.toString(),
                label = stringResource(R.string.tasks_seg2_figure_in_progress),
                onClick = { onFilterChange(TaskFilter.IN_PROGRESS) }
            ),
            PanelFigure(
                value = state.pausedCount.toString(),
                label = stringResource(R.string.tasks_seg2_figure_paused),
                onClick = { onFilterChange(TaskFilter.PAUSED) }
            )
        ),
        loading = loading
    )
}

@Composable
private fun TaskFilterRow(state: MyTasksUiState, onFilterChange: (TaskFilter) -> Unit) {
    val filters = listOf(
        TaskFilter.ALL,
        TaskFilter.IN_PROGRESS,
        TaskFilter.PAUSED,
        TaskFilter.ASSIGNED,
        TaskFilter.SUSPENDED
    )
    val labels = mapOf(
        TaskFilter.ALL to stringResource(R.string.orders_filter_all),
        TaskFilter.IN_PROGRESS to stringResource(R.string.task_status_in_progress),
        TaskFilter.PAUSED to stringResource(R.string.task_status_on_hold),
        TaskFilter.ASSIGNED to stringResource(R.string.tasks_seg2_filter_assigned),
        TaskFilter.SUSPENDED to stringResource(R.string.tasks_seg2_filter_suspended)
    )
    val mechanicPausedCount = state.tasks.count { it.isPausedByMechanic }
    val suspendedCount = state.tasks.count { it.isSuspended || it.hasPendingSuspension }

    AtelierSegmentedControl(
        options = filters,
        selected = state.selectedFilter,
        onSelect = onFilterChange,
        label = { labels[it].orEmpty() },
        badge = { filter ->
            when (filter) {
                TaskFilter.ALL -> 0
                TaskFilter.IN_PROGRESS -> state.inProgressCount
                TaskFilter.PAUSED -> mechanicPausedCount
                TaskFilter.ASSIGNED -> state.pendingCount
                TaskFilter.SUSPENDED -> suspendedCount
            }
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun TaskRow(task: Task, onClick: () -> Unit) {
    val meta = listOfNotNull(
        task.bayName?.let { stringResource(R.string.task_card_bay, it) },
        task.workOrderNumber?.let { stringResource(R.string.task_card_work_order, it) },
        task.estimatedHours?.let {
            stringResource(R.string.task_card_estimated, it.stripTrailingZeros().toPlainString())
        }
    ).joinToString(" · ").ifEmpty { null }

    val (chipLabelRes, chipKind) = if (task.hasPendingSuspension) {
        R.string.task_suspension_pending to ChipKind.Wait
    } else {
        taskStatusLabelRes(task.status, task.holdReason) to taskStatusChipKind(task.status, task.holdReason)
    }

    AtelierListRow(
        title = task.serviceName ?: task.description,
        subtitle = meta ?: task.description.takeIf { task.serviceName != null },
        plate = task.plate,
        leading = if (task.plate == null) {
            {
                Text(
                    text = stringResource(R.string.tasks_seg2_meta_no_plate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        onClick = onClick,
        trailing = { AtelierChip(text = stringResource(chipLabelRes), kind = chipKind) }
    )
}

// region Previews

private fun previewTask(
    id: String,
    plate: String?,
    status: TaskStatus,
    hold: HoldReason? = null,
    pendingSuspension: String? = null
) = Task(
    id = TaskId(id),
    workOrderId = "wo1",
    workOrderNumber = 1042,
    workOrderStatus = "IN_PROGRESS",
    vehicleId = "v1",
    plate = plate,
    branchId = "b1",
    bayId = "bay1",
    bayName = "B-2",
    serviceName = null,
    mechanicId = "m1",
    status = status,
    description = "Cambio de pastillas de freno delanteras",
    estimatedHours = BigDecimal("1.5"),
    holdReason = hold,
    missingItemDescription = null,
    pendingSuspensionReason = pendingSuspension,
    startedAt = null,
    totalPausedSeconds = 0
)

private val previewTasks = listOf(
    previewTask("t1", "BKL482", TaskStatus.IN_PROGRESS),
    previewTask("t2", "F4T119", TaskStatus.ON_HOLD, HoldReason.BREAK),
    previewTask("t3", "ABC123", TaskStatus.ASSIGNED),
    previewTask("t4", "D9K204", TaskStatus.ON_HOLD, HoldReason.WAITING_PARTS, "Falta retén")
)

@Preview(name = "Mis tareas · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MyTasksPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    MyTasksContent(
        state = MyTasksUiState(
            isLoaded = true,
            tenantName = "Taller Suárez · Chorrillos",
            tasks = previewTasks
        ),
        onBack = null,
        onRefresh = {},
        onFilterChange = {},
        onOpenTask = {}
    )
}

@Preview(
    name = "Mis tareas · oscuro",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MyTasksPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    MyTasksContent(
        state = MyTasksUiState(
            isLoaded = true,
            tenantName = "Taller Suárez · Chorrillos",
            tasks = previewTasks,
            showingCache = true
        ),
        onBack = {},
        onRefresh = {},
        onFilterChange = {},
        onOpenTask = {}
    )
}

@Preview(name = "Mis tareas · vacío", widthDp = 390, heightDp = 640, showBackground = true)
@Composable
private fun MyTasksPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    MyTasksContent(
        state = MyTasksUiState(isLoaded = true, tenantName = "Taller Suárez"),
        onBack = {},
        onRefresh = {},
        onFilterChange = {},
        onOpenTask = {}
    )
}

// endregion
