package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import java.math.BigDecimal

/** US26: bandeja de tareas del mecánico. [onOpenTask] recibe el id de la orden y el de la tarea. */
@Composable
fun MyTasksScreen(
    onBack: () -> Unit,
    onOpenTask: (workOrderId: String, taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyTasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyTasksContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenTask = { onOpenTask(it.workOrderId, it.id.value) },
        modifier = modifier
    )
}

@Composable
internal fun MyTasksContent(
    state: MyTasksUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.tasks_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.tasks_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.canRead) {
                AtelierBanner(
                    message = stringResource(R.string.tasks_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            if (state.showingCache) {
                AtelierBanner(
                    message = state.error?.asString() ?: stringResource(R.string.tasks_cache_notice),
                    icon = Icons.Outlined.WifiOff,
                    kind = BannerKind.Wait
                )
            }
            when {
                !state.isLoaded -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                state.tasks.isEmpty() -> Text(
                    text = stringResource(R.string.tasks_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                else -> state.tasks.forEach { task -> TaskCard(task, onClick = { onOpenTask(task) }) }
            }
            AtelierButton(
                text = stringResource(R.string.tasks_refresh),
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                loading = state.isRefreshing
            )
        }
    }
}

private fun previewTask(id: String, status: TaskStatus, hold: HoldReason? = null) = Task(
    id = TaskId(id), workOrderId = "wo1", workOrderNumber = 1042, workOrderStatus = "IN_PROGRESS",
    vehicleId = "v1", plate = "ABC-123", branchId = "b1", bayId = "bay1", bayName = "B-2", serviceName = null,
    mechanicId = "m1", status = status, description = "Cambio de pastillas de freno delanteras",
    estimatedHours = BigDecimal("1.5"), holdReason = hold, missingItemDescription = null,
    pendingSuspensionReason = null, startedAt = null, totalPausedSeconds = 0
)

@Preview(name = "Mis tareas · oscuro", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun MyTasksPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    MyTasksContent(
        state = MyTasksUiState(
            isLoaded = true,
            tasks = listOf(
                previewTask("t1", TaskStatus.IN_PROGRESS),
                previewTask("t2", TaskStatus.ON_HOLD, HoldReason.BREAK),
                previewTask("t3", TaskStatus.ASSIGNED)
            )
        ),
        onBack = {}, onRefresh = {}, onOpenTask = {}
    )
}

@Preview(name = "Mis tareas · vacío · claro", widthDp = 360, heightDp = 600, showBackground = true)
@Composable
private fun MyTasksPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    MyTasksContent(state = MyTasksUiState(isLoaded = true), onBack = {}, onRefresh = {}, onOpenTask = {})
}
