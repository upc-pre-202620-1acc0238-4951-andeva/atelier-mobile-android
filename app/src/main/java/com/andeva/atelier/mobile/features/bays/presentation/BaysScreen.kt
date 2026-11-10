package com.andeva.atelier.mobile.features.bays.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.designsystem.theme.PillShape
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.bays_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.canReadBays) {
                AtelierBanner(
                    message = stringResource(R.string.bays_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }

            state.notice?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
            }
            state.error?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Filled.Error)
            }

            if (state.canManageBays) {
                AtelierButton(
                    text = stringResource(R.string.bays_new_btn),
                    onClick = onOpenCreateBay,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Add
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChipButton(
                    text = stringResource(R.string.bays_filter_all, state.board.size),
                    selected = state.statusFilter == null,
                    onClick = { onFilterChange(null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChipButton(
                    text = stringResource(R.string.bays_filter_available, state.availableCount),
                    selected = state.statusFilter == BayStatus.AVAILABLE,
                    onClick = { onFilterChange(BayStatus.AVAILABLE) },
                    modifier = Modifier.weight(1f)
                )
                FilterChipButton(
                    text = stringResource(R.string.bays_filter_occupied, state.occupiedCount),
                    selected = state.statusFilter == BayStatus.OCCUPIED,
                    onClick = { onFilterChange(BayStatus.OCCUPIED) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.filteredBoard.isEmpty() && !state.isRefreshing) {
                AtelierBanner(
                    message = stringResource(R.string.bays_empty),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
            } else {
                state.filteredBoard.forEach { item ->
                    BayCard(
                        item = item,
                        canAssign = state.canAssignBay && item.bay.canReceive && state.assignableOrders.isNotEmpty(),
                        onOpenOrder = onOpenOrder,
                        onAssignClick = { onOpenAssignOrder(item.bay) }
                    )
                }
            }

            AtelierButton(
                text = stringResource(R.string.bays_refresh),
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                loading = state.isRefreshing
            )
        }
    }

    if (state.showCreateDialog) {
        CreateBayDialog(
            isSaving = state.isCreatingBay,
            onConfirm = onCreateBay,
            onDismiss = onDismissCreateBay
        )
    }

    state.targetBayForAssign?.let { bay ->
        AlertDialog(
            onDismissRequest = onDismissAssignOrder,
            confirmButton = {},
            dismissButton = {
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = onDismissAssignOrder,
                    style = AtelierButtonStyle.Text
                )
            },
            title = { Text(stringResource(R.string.order_assign_to_bay, bay.name)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bays_select_order_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    state.assignableOrders.forEach { order ->
                        val code = order.internalNumber?.let { stringResource(R.string.order_code_format, it) }
                            ?: order.id.take(8)
                        val titleText = if (!order.plate.isNullOrBlank()) "$code · ${order.plate}" else code
                        AtelierSelectionCard(
                            title = titleText,
                            subtitle = order.customerName,
                            badgeText = stringResource(order.status.labelRes()),
                            enabled = !state.isAssigning,
                            onClick = { onAssignOrderToBay(order.id) }
                        )
                    }
                }
            }
        )
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

@Composable
private fun CreateBayDialog(
    isSaving: Boolean,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AtelierButton(
                text = stringResource(R.string.bays_create_confirm),
                onClick = { onConfirm(name, selectedType) },
                enabled = name.isNotBlank() && !isSaving,
                loading = isSaving
            )
        },
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        },
        title = { Text(stringResource(R.string.bays_create_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    val isSelected = type == selectedType
                    Surface(
                        onClick = { selectedType = type },
                        shape = PillShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        enabled = !isSaving
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(type.labelRes()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun FilterChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(38.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BayCard(
    item: BayBoardItem,
    canAssign: Boolean,
    onOpenOrder: (String) -> Unit,
    onAssignClick: () -> Unit
) {
    val bay = item.bay
    val occupant = item.occupant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(enabled = occupant != null) { occupant?.workOrderId?.let(onOpenOrder) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = bay.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            AtelierChip(
                text = when (bay.status) {
                    BayStatus.AVAILABLE -> stringResource(R.string.bay_status_available)
                    BayStatus.OCCUPIED -> stringResource(R.string.bay_status_occupied)
                    BayStatus.MAINTENANCE -> stringResource(R.string.bay_status_maintenance)
                    null -> ""
                },
                kind = when (bay.status) {
                    BayStatus.AVAILABLE -> ChipKind.Ok
                    BayStatus.OCCUPIED -> ChipKind.Info
                    BayStatus.MAINTENANCE -> ChipKind.Wait
                    null -> ChipKind.Idle
                }
            )
        }
        bay.type?.let {
            Text(text = stringResource(it.labelRes()), style = MaterialTheme.typography.bodySmall, color = AtelierTheme.colors.muted)
        }

        if (occupant != null) {
            val code = occupant.internalNumber?.let { stringResource(R.string.order_code_format, it) }
                ?: occupant.workOrderId.take(8)
            val line = listOfNotNull(code, occupant.plate).joinToString(" · ")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            val statusDesc = when (bay.status) {
                BayStatus.AVAILABLE -> stringResource(R.string.bay_status_available_desc)
                BayStatus.OCCUPIED -> stringResource(R.string.bay_status_occupied_desc)
                BayStatus.MAINTENANCE -> stringResource(R.string.bay_status_maintenance_desc)
                null -> ""
            }
            if (statusDesc.isNotBlank()) {
                Text(
                    text = statusDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (bay.status) {
                        BayStatus.AVAILABLE -> AtelierTheme.colors.ok
                        BayStatus.MAINTENANCE -> AtelierTheme.colors.bad
                        else -> AtelierTheme.colors.inkSecondary
                    }
                )
            }
        }

        if (canAssign) {
            AtelierButton(
                text = stringResource(R.string.bays_assign_order_btn),
                onClick = onAssignClick,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Tonal
            )
        }
    }
}
