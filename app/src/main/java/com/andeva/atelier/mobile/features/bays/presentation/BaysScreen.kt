package com.andeva.atelier.mobile.features.bays.presentation

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.time.Duration

/**
 * Tablero de bahías de la sede: cuál está libre, cuál ocupada y con qué auto. Tocar una bahía ocupada abre su
 * orden; tocar una libre (con permiso de reasignar) abre la hoja para ingresar una orden. Ambas acciones son en
 * línea: sin red se avisa y no se encola nada.
 */
@Composable
fun BaysScreen(
    onBack: () -> Unit,
    onOpenOrder: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BaysViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BaysContent(
        state = state,
        onBack = onBack,
        onFilterChange = viewModel::onFilterChange,
        onRefresh = viewModel::refresh,
        onOpenOrder = onOpenOrder,
        onOpenAssignOrder = viewModel::onOpenAssignOrder,
        onDismissAssignOrder = viewModel::onDismissAssignOrder,
        onAssignOrderToBay = viewModel::onAssignOrderToBay,
        onOpenCreateBay = viewModel::onOpenCreateBay,
        onDismissCreateBay = viewModel::onDismissCreateBay,
        onCreateBay = viewModel::onCreateBay,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BaysContent(
    state: BaysUiState,
    onBack: () -> Unit,
    onFilterChange: (BayStatus?) -> Unit,
    onRefresh: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenAssignOrder: (WorkBay) -> Unit,
    onDismissAssignOrder: () -> Unit,
    onAssignOrderToBay: (String) -> Unit,
    onOpenCreateBay: () -> Unit,
    onDismissCreateBay: () -> Unit,
    onCreateBay: (String, BayType) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.bays_title), onBack = onBack) },
        floatingActionButton = {
            if (state.canManageBays && state.isOnline) {
                AtelierFab(
                    text = stringResource(R.string.bays_new_btn),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenCreateBay
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!state.canReadBays) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.bays_no_permission),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            BayFilters(state, onFilterChange)
            AtelierRefreshBar(visible = state.isRefreshing && state.hasLoaded)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing && state.hasLoaded,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Deja libre el alto del FAB para que la última fila no quede debajo.
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (!state.isOnline) {
                        item(key = "offline") {
                            AtelierBanner(
                                message = stringResource(R.string.bays_offline),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            )
                        }
                    }
                    state.notice?.let {
                        item(key = "notice") {
                            AtelierBanner(
                                message = it.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            )
                        }
                    }
                    val error = state.error
                    if (error != null && state.board.isNotEmpty() && state.targetBayForAssign == null && !state.showCreateDialog) {
                        item(key = "error") {
                            AtelierBanner(
                                message = error.asString(),
                                icon = Icons.Outlined.ErrorOutline,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            )
                        }
                    }
                    when {
                        state.showSkeleton -> items(4, key = { "bone-$it" }) {
                            AtelierListRow(title = "Bahía", subtitle = "Elevador", plate = "ABC123", loading = true)
                        }
                        state.board.isEmpty() && error != null && !state.isOnline -> item(key = "offline-state") {
                            AtelierOfflineState(modifier = Modifier.padding(horizontal = 16.dp), onRetry = onRefresh)
                        }
                        state.board.isEmpty() && error != null -> item(key = "error-state") {
                            AtelierErrorState(
                                message = error.asString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onRetry = onRefresh
                            )
                        }
                        state.filteredBoard.isEmpty() -> item(key = "empty") {
                            AtelierEmptyState(
                                text = stringResource(R.string.bays_empty),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        else -> items(state.filteredBoard, key = { it.bay.id }) { item ->
                            BayRow(
                                item = item,
                                canAssign = state.canAssignBay && state.isOnline && item.bay.canReceive &&
                                    state.assignableOrders.isNotEmpty(),
                                onOpenOrder = onOpenOrder,
                                onAssign = { onOpenAssignOrder(item.bay) }
                            )
                        }
                    }
                }
            }
        }
    }

    state.targetBayForAssign?.let { bay ->
        AssignOrderSheet(state = state, bay = bay, onDismiss = onDismissAssignOrder, onAssign = onAssignOrderToBay)
    }
    if (state.showCreateDialog) {
        CreateBaySheet(
            isSaving = state.isCreatingBay,
            isOnline = state.isOnline,
            errorText = state.error?.asString(),
            onConfirm = onCreateBay,
            onDismiss = onDismissCreateBay
        )
    }
}

/** Filtro del tablero: pestañas con su conteo, para que el estado se lea en texto y no solo en color. */
@Composable
private fun BayFilters(state: BaysUiState, onFilterChange: (BayStatus?) -> Unit) {
    val filters = listOf(
        null to stringResource(R.string.bays_filter_all, state.board.size),
        BayStatus.AVAILABLE to stringResource(R.string.bays_filter_available, state.availableCount),
        BayStatus.OCCUPIED to stringResource(R.string.bays_filter_occupied, state.occupiedCount)
    )
    PrimaryTabRow(
        selectedTabIndex = filters.indexOfFirst { it.first == state.statusFilter }.coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        filters.forEach { (status, label) ->
            Tab(
                selected = state.statusFilter == status,
                onClick = { onFilterChange(status) },
                text = { Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun BayRow(
    item: BayBoardItem,
    canAssign: Boolean,
    onOpenOrder: (String) -> Unit,
    onAssign: () -> Unit
) {
    val bay = item.bay
    val occupant = item.occupant
    val line = when {
        occupant != null -> listOfNotNull(
            bay.type?.let { stringResource(it.labelRes()) },
            occupant.internalNumber?.let { stringResource(R.string.today_order_number, it) },
            occupant.laborElapsed?.let { stringResource(R.string.bays_labor, it.formatElapsed()) }
        ).joinToString(" · ")
        canAssign -> listOfNotNull(
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
            occupant != null -> ({ onOpenOrder(occupant.workOrderId) })
            canAssign -> onAssign
            else -> null
        },
        trailing = { bay.status?.let { BayStatusChip(it) } }
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

/** Elige qué orden entra a la bahía libre. Reasignar es en línea. */
@Composable
private fun AssignOrderSheet(
    state: BaysUiState,
    bay: WorkBay,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.order_assign_to_bay, bay.name),
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(R.string.bays_select_order_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        if (!state.isOnline) AtelierRequiresConnection()
        state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        state.assignableOrders.forEach { order ->
            val code = order.internalNumber?.let { stringResource(R.string.today_order_number, it) }
                ?: stringResource(R.string.today_order_no_number)
            AtelierSelectionCard(
                title = listOfNotNull(order.plate, code).joinToString(" · "),
                subtitle = order.customerName,
                badgeText = stringResource(order.status.labelRes()),
                enabled = state.isOnline && !state.isAssigning,
                onClick = { onAssign(order.id) }
            )
        }
    }
}

/** Registra una bahía nueva (`BAYS_MANAGE`). En línea. */
@Composable
private fun CreateBaySheet(
    isSaving: Boolean,
    isOnline: Boolean,
    errorText: String?,
    onConfirm: (name: String, type: BayType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BayType.LIFT) }
    val types = listOf(
        BayType.LIFT,
        BayType.MECHANICAL_LIFT,
        BayType.DIAGNOSTIC_PIT,
        BayType.WASH_BAY,
        BayType.ALIGNMENT_STATION,
        BayType.PAINT_BOOTH
    )
    AtelierBottomSheet(
        title = stringResource(R.string.bays_create_dialog_title),
        onDismiss = { if (!isSaving) onDismiss() },
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.bays_create_confirm),
                onClick = { onConfirm(name, selectedType) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && isOnline && !isSaving,
                loading = isSaving
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.bays_name_label),
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.bays_name_hint),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
        Text(
            text = stringResource(R.string.bays_type_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        types.forEach { type ->
            AtelierSelectionCard(
                title = stringResource(type.labelRes()),
                selected = type == selectedType,
                enabled = !isSaving,
                onClick = { selectedType = type }
            )
        }
        errorText?.let { AtelierBanner(message = it, icon = Icons.Outlined.ErrorOutline) }
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

/** `1 h 20 min` / `35 min`: el tiempo de mano de obra de las tareas en curso. */
private fun Duration.formatElapsed(): String {
    val minutes = toMinutes()
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
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
    canManageBays = true
)

@Composable
private fun PreviewContent(state: BaysUiState) {
    BaysContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> })
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

@Preview(name = "Bahías · elegir orden", widthDp = 390, heightDp = 844)
@Composable
private fun BaysPreviewAssign() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(targetBayForAssign = previewState.board[2].bay))
}

// endregion
