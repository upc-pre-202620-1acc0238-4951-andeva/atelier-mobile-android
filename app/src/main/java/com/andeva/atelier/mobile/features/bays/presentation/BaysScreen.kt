package com.andeva.atelier.mobile.features.bays.presentation

import android.content.res.Configuration
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.bays.domain.laborElapsed
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Tablero de bahías de la sede: cuál está libre, cuál ocupada, cuál en mantenimiento y con qué auto.
 * Todas las acciones de asignación, liberación, mantenimiento y restauración son en línea.
 */
@Composable
fun BaysScreen(
    onBack: () -> Unit,
    onOpenOrder: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BaysViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pollWhileVisible()
        }
    }

    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }

    BaysContent(
        state = state,
        actions = BaysActions(
            onBack = onBack,
            onFilterChange = viewModel::onFilterChange,
            onRefresh = viewModel::refresh,
            onOpenOrder = onOpenOrder,
            onOpenBayActions = viewModel::onOpenBayActions,
            onDismissBayActions = viewModel::onDismissBayActions,
            onAssignOrderToBay = viewModel::onAssignOrderToBay,
            onReassignSelectedBayOrder = viewModel::onReassignSelectedBayOrder,
            onReleaseSelectedBay = viewModel::onReleaseSelectedBay,
            onOpenReassignTaskMechanic = viewModel::onOpenReassignTaskMechanic,
            onConfirmReassignTaskMechanic = viewModel::onConfirmReassignTaskMechanic,
            onToggleMaintenanceInput = viewModel::onToggleMaintenanceInput,
            onMaintenanceReasonChange = viewModel::onMaintenanceReasonChange,
            onConfirmSetMaintenance = viewModel::onConfirmSetMaintenance,
            onRestoreSelectedBay = viewModel::onRestoreSelectedBay,
            onDismissAssignOrder = viewModel::onDismissAssignOrder,
            onOpenCreateBay = viewModel::onOpenCreateBay,
            onDismissCreateBay = viewModel::onDismissCreateBay,
            onCreateBay = viewModel::onCreateBay
        ),
        modifier = modifier
    )
}

internal class BaysActions(
    val onBack: () -> Unit = {},
    val onFilterChange: (BayStatus?) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpenOrder: (String) -> Unit = {},
    val onOpenBayActions: (BayBoardItem) -> Unit = {},
    val onDismissBayActions: () -> Unit = {},
    val onAssignOrderToBay: (String) -> Unit = {},
    val onReassignSelectedBayOrder: (WorkBay) -> Unit = {},
    val onReleaseSelectedBay: () -> Unit = {},
    val onOpenReassignTaskMechanic: (String?) -> Unit = {},
    val onConfirmReassignTaskMechanic: (String, String) -> Unit = { _, _ -> },
    val onToggleMaintenanceInput: (Boolean) -> Unit = {},
    val onMaintenanceReasonChange: (String) -> Unit = {},
    val onConfirmSetMaintenance: () -> Unit = {},
    val onRestoreSelectedBay: () -> Unit = {},
    val onDismissAssignOrder: () -> Unit = {},
    val onOpenCreateBay: () -> Unit = {},
    val onDismissCreateBay: () -> Unit = {},
    val onCreateBay: (String, BayType) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BaysContent(
    state: BaysUiState,
    actions: BaysActions,
    modifier: Modifier = Modifier
) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton && state.canReadBays)
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(state.occupiedCount) {
        if (state.occupiedCount > 0) {
            while (true) {
                now = Instant.now()
                delay(30_000L)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.bays_title), onBack = actions.onBack) },
        floatingActionButton = {
            if (state.canManageBays && state.isOnline) {
                AtelierFab(
                    text = stringResource(R.string.bays_new_btn),
                    icon = Icons.Outlined.Add,
                    onClick = actions.onOpenCreateBay
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!state.canReadBays) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.bays_no_permission),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                return@Column
            }
            BayFilters(state, actions.onFilterChange)
            AtelierRefreshBar(visible = state.isRefreshing && state.hasLoaded)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing && state.hasLoaded,
                onRefresh = actions.onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .floatingBarPadding(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (!state.isOnline) {
                        item(key = "offline") {
                            AtelierBanner(
                                message = stringResource(R.string.bays_offline),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info,
                                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
                            )
                        }
                    }
                    state.notice?.let {
                        item(key = "notice") {
                            AtelierBanner(
                                message = it.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
                            )
                        }
                    }
                    val error = state.error
                    if (error != null && state.board.isNotEmpty() && !state.hasOpenSheet) {
                        item(key = "error") {
                            AtelierBanner(
                                message = error.asString(),
                                icon = Icons.Outlined.ErrorOutline,
                                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
                            )
                        }
                    }
                    if (!showSkeleton && !state.showSkeleton && state.board.isNotEmpty()) {
                        item(key = "occupancy-summary") {
                            BayOccupancySummary(state)
                        }
                    }
                    when {
                        showSkeleton -> items(4, key = { "bone-$it" }) {
                            AtelierListRow(title = "Bahía", subtitle = "Elevador", plate = "ABC123", loading = true)
                        }
                        state.showSkeleton -> Unit
                        state.board.isEmpty() && error != null && !state.isOnline -> item(key = "offline-state") {
                            AtelierOfflineState(modifier = Modifier.padding(horizontal = Spacing.md), onRetry = actions.onRefresh)
                        }
                        state.board.isEmpty() && error != null -> item(key = "error-state") {
                            AtelierErrorState(
                                message = error.asString(),
                                modifier = Modifier.padding(horizontal = Spacing.md),
                                onRetry = actions.onRefresh
                            )
                        }
                        state.filteredBoard.isEmpty() -> item(key = "empty") {
                            AtelierEmptyState(
                                text = stringResource(R.string.bays_empty),
                                modifier = Modifier.padding(horizontal = Spacing.md)
                            )
                        }
                        else -> items(state.filteredBoard, key = { it.bay.id }) { item ->
                            val liveElapsed = item.occupant?.let { occ ->
                                if (occ.tasks.isNotEmpty()) laborElapsed(occ.tasks, now) else occ.laborElapsed
                            }
                            BayRow(
                                item = item,
                                state = state,
                                liveElapsed = liveElapsed,
                                onOpenOrder = actions.onOpenOrder,
                                onOpenActions = { actions.onOpenBayActions(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    state.selectedBayItem?.let { item ->
        val liveElapsed = item.occupant?.let { occ ->
            if (occ.tasks.isNotEmpty()) laborElapsed(occ.tasks, now) else occ.laborElapsed
        }
        BayActionsSheet(
            item = item,
            state = state,
            liveElapsed = liveElapsed,
            onOpenOrder = { orderId ->
                actions.onDismissBayActions()
                actions.onOpenOrder(orderId)
            },
            onAssignOrder = actions.onAssignOrderToBay,
            onReassignOrder = actions.onReassignSelectedBayOrder,
            onReleaseBay = actions.onReleaseSelectedBay,
            onOpenReassignTaskMechanic = actions.onOpenReassignTaskMechanic,
            onConfirmReassignTaskMechanic = actions.onConfirmReassignTaskMechanic,
            onToggleMaintenanceInput = actions.onToggleMaintenanceInput,
            onMaintenanceReasonChange = actions.onMaintenanceReasonChange,
            onConfirmMaintenance = actions.onConfirmSetMaintenance,
            onRestoreBay = actions.onRestoreSelectedBay,
            onDismiss = actions.onDismissBayActions
        )
    }

    state.targetBayForAssign?.let { bay ->
        AssignOrderSheet(
            state = state,
            bay = bay,
            onDismiss = actions.onDismissAssignOrder,
            onAssign = actions.onAssignOrderToBay
        )
    }

    if (state.showCreateDialog) {
        CreateBaySheet(
            isSaving = state.isCreatingBay,
            isOnline = state.isOnline,
            errorText = state.error?.asString(),
            onConfirm = actions.onCreateBay,
            onDismiss = actions.onDismissCreateBay
        )
    }
}

private enum class BayFilterOption(val status: BayStatus?) {
    ALL(null),
    AVAILABLE(BayStatus.AVAILABLE),
    OCCUPIED(BayStatus.OCCUPIED),
    MAINTENANCE(BayStatus.MAINTENANCE)
}

/** Filtro del tablero en pista hundida con cápsula deslizante y conteo por estado. */
@Composable
private fun BayFilters(state: BaysUiState, onFilterChange: (BayStatus?) -> Unit) {
    val selected = BayFilterOption.entries.firstOrNull { it.status == state.statusFilter } ?: BayFilterOption.ALL
    val allLabel = stringResource(R.string.bays_filter_all_short)
    val availableLabel = stringResource(R.string.bays_filter_available_short)
    val occupiedLabel = stringResource(R.string.bays_filter_occupied_short)
    val maintenanceLabel = stringResource(R.string.bays_filter_maintenance_short)
    AtelierSegmentedControl(
        options = BayFilterOption.entries,
        selected = selected,
        onSelect = { onFilterChange(it.status) },
        label = { option ->
            when (option) {
                BayFilterOption.ALL -> allLabel
                BayFilterOption.AVAILABLE -> availableLabel
                BayFilterOption.OCCUPIED -> occupiedLabel
                BayFilterOption.MAINTENANCE -> maintenanceLabel
            }
        },
        badge = { option ->
            when (option) {
                BayFilterOption.ALL -> state.board.size
                BayFilterOption.AVAILABLE -> state.availableCount
                BayFilterOption.OCCUPIED -> state.occupiedCount
                BayFilterOption.MAINTENANCE -> state.maintenanceCount
            }
        },
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}

@Composable
private fun BayOccupancySummary(state: BaysUiState) {
    val colors = AtelierTheme.colors
    val total = state.board.size
    val progress = if (total > 0) state.occupiedCount.toFloat() / total.toFloat() else 0f
    val barColor = when {
        progress >= 0.85f -> colors.warnMark
        state.occupiedCount > 0 -> colors.info
        else -> colors.okMark
    }
    AtelierSurface(
        modifier = Modifier
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.occupiedCount > 0) {
                        AtelierLiveBeacon(color = colors.info, size = 8.dp)
                    }
                    Text(
                        text = stringResource(R.string.bays_occupancy_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.bays_occupancy_fraction, state.occupiedCount, total),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AtelierMetricBar(
                progress = progress,
                color = barColor,
                height = 8.dp
            )
            Text(
                text = stringResource(R.string.bays_occupancy_breakdown, state.availableCount, state.maintenanceCount),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )
        }
    }
}

@Composable
private fun BayRow(
    item: BayBoardItem,
    state: BaysUiState,
    liveElapsed: Duration?,
    onOpenOrder: (String) -> Unit,
    onOpenActions: () -> Unit
) {
    val colors = AtelierTheme.colors
    val bay = item.bay
    val occupant = item.occupant
    val canAssignThisBay = state.canAssignBay && state.isOnline && bay.canReceive && state.assignableOrders.isNotEmpty()
    val canActOnBay = when (bay.status) {
        BayStatus.OCCUPIED -> occupant != null && (state.canReadWorkOrders || state.canReleaseBay || state.canAssignBay)
        BayStatus.AVAILABLE -> canAssignThisBay || (state.canManageBays && state.isOnline)
        BayStatus.MAINTENANCE -> state.canManageBays && state.isOnline
        null -> false
    }
    val elapsed = liveElapsed ?: occupant?.laborElapsed
    val mechanicNames = occupant?.let { state.mechanicLabelsFor(it) }.orEmpty()
    val line = when {
        occupant != null -> listOfNotNull(
            bay.type?.let { stringResource(it.labelRes()) },
            occupant.internalNumber?.let { stringResource(R.string.today_order_number, it) },
            elapsed?.let { stringResource(R.string.bays_labor, it.formatBayElapsed()) },
            mechanicNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
        ).joinToString(" · ")
        canAssignThisBay -> listOfNotNull(
            bay.type?.let { stringResource(it.labelRes()) },
            stringResource(R.string.bays_tap_to_assign)
        ).joinToString(" · ")
        else -> bay.type?.let { stringResource(it.labelRes()) }.orEmpty()
    }
    AtelierListRow(
        title = bay.name,
        subtitle = line.ifEmpty { null },
        plate = occupant?.plate,
        onClick = when {
            canActOnBay -> onOpenActions
            occupant != null && state.canReadWorkOrders -> ({ onOpenOrder(occupant.workOrderId) })
            else -> null
        },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bay.status == BayStatus.OCCUPIED) {
                    AtelierLiveBeacon(color = colors.info, size = 8.dp)
                }
                bay.status?.let { BayStatusChip(it) }
            }
        }
    )
}

@Composable
private fun BayStatusChip(status: BayStatus) {
    when (status) {
        BayStatus.AVAILABLE -> AtelierChip(stringResource(R.string.bay_status_available), kind = ChipKind.Ok)
        BayStatus.OCCUPIED -> AtelierChip(stringResource(R.string.bay_status_occupied), kind = ChipKind.Info)
        BayStatus.MAINTENANCE ->
            AtelierChip(stringResource(R.string.bay_status_maintenance), kind = ChipKind.Warn, icon = Icons.Outlined.Build)
    }
}

@StringRes
internal fun BayType.labelRes(): Int = when (this) {
    BayType.LIFT -> R.string.bay_type_lift
    BayType.MECHANICAL_LIFT -> R.string.bay_type_mechanical_lift
    BayType.DIAGNOSTIC_PIT -> R.string.bay_type_diagnostic_pit
    BayType.WASHING, BayType.WASH_BAY -> R.string.bay_type_wash_bay
    BayType.ALIGNMENT, BayType.ALIGNMENT_STATION -> R.string.bay_type_alignment
    BayType.PAINT_BOOTH -> R.string.bay_type_paint_booth
}

// region Previews

private fun previewBay(id: String, name: String, type: BayType, status: BayStatus) =
    WorkBay(id = id, branchId = "b1", name = name, type = type, status = status, currentWorkOrderId = null)

private fun previewOrder(id: String, number: Int, plate: String, customer: String, status: WorkOrderStatus) = WorkOrder(
    id = id, internalNumber = number, branchId = "b1", vehicleId = "v$id", customerId = "c$id",
    currentBayId = null, bayName = null, status = status, diagnosticSummary = null, mileageIn = null,
    subtotal = null, taxAmount = null, totalAmount = Money.of("480.00", Money.PEN), plate = plate, customerName = customer
)

private val previewState = BaysUiState(
    board = listOf(
        BayBoardItem(
            previewBay("1", "Elevador 1", BayType.LIFT, BayStatus.OCCUPIED),
            BayOccupant("o1", 1041, "F5U731", listOf("m1"), Duration.ofMinutes(80))
        ),
        BayBoardItem(
            previewBay("2", "Elevador 2", BayType.LIFT, BayStatus.OCCUPIED),
            BayOccupant("o2", 1040, "AXT209", listOf("m2"), null)
        ),
        BayBoardItem(previewBay("3", "Fosa de diagnóstico", BayType.DIAGNOSTIC_PIT, BayStatus.AVAILABLE), null),
        BayBoardItem(previewBay("4", "Alineación", BayType.ALIGNMENT_STATION, BayStatus.MAINTENANCE), null)
    ),
    activeOrders = listOf(
        previewOrder("o3", 1042, "BKR418", "Rosa Quispe Mamani", WorkOrderStatus.DRAFT)
    ),
    hasLoaded = true,
    canReadBays = true,
    canAssignBay = true,
    canReleaseBay = true,
    canManageBays = true
)

@Composable
private fun PreviewContent(state: BaysUiState) {
    BaysContent(state = state, actions = BaysActions())
}

@Preview(name = "Bahías · claro", widthDp = 390, heightDp = 844)
@Composable
private fun BaysPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Bahías · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BaysPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Bahías · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun BaysPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(BaysUiState(canReadBays = true))
}

@Preview(name = "Bahías · hoja ocupada · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BaysPreviewOccupiedSheet() = AtelierWorkshopTheme(darkTheme = true) {
    PreviewContent(previewState.copy(selectedBayItem = previewState.board[0]))
}

// endregion
