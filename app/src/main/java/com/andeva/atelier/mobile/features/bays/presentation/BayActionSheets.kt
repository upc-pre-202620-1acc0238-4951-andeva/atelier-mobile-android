package com.andeva.atelier.mobile.features.bays.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.time.Duration

/** Hoja inferior contextual de una bahía: ocupada (abrir orden, liberar, reasignar bahía o mecánico), libre o en mantenimiento. */
@Composable
internal fun BayActionsSheet(
    item: BayBoardItem,
    state: BaysUiState,
    liveElapsed: Duration?,
    onOpenOrder: (String) -> Unit,
    onAssignOrder: (String) -> Unit,
    onReassignOrder: (WorkBay) -> Unit,
    onReleaseBay: () -> Unit,
    onOpenReassignTaskMechanic: (String?) -> Unit,
    onConfirmReassignTaskMechanic: (taskId: String, mechanicId: String) -> Unit,
    onToggleMaintenanceInput: (Boolean) -> Unit,
    onMaintenanceReasonChange: (String) -> Unit,
    onConfirmMaintenance: () -> Unit,
    onRestoreBay: () -> Unit,
    onDismiss: () -> Unit
) {
    val bay = item.bay
    val occupant = item.occupant
    val busy = state.isUpdatingBay || state.isAssigning

    val title = when (bay.status) {
        BayStatus.OCCUPIED -> stringResource(R.string.bays_occupied_sheet_title)
        BayStatus.AVAILABLE -> stringResource(R.string.bays_available_sheet_title)
        BayStatus.MAINTENANCE -> stringResource(R.string.bays_maintenance_sheet_title)
        null -> bay.name
    }

    AtelierBottomSheet(
        title = title,
        onDismiss = { if (!busy) onDismiss() },
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            when (bay.status) {
                BayStatus.OCCUPIED -> {
                    if (occupant != null) {
                        if (state.canReadWorkOrders) {
                            AtelierButton(
                                text = stringResource(R.string.bays_open_order_btn),
                                onClick = { onOpenOrder(occupant.workOrderId) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy
                            )
                        }
                        if (state.canReleaseBay) {
                            AtelierButton(
                                text = stringResource(R.string.bays_release_btn),
                                onClick = onReleaseBay,
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.Destructive,
                                enabled = state.isOnline && !busy,
                                loading = state.isUpdatingBay
                            )
                        }
                    }
                }
                BayStatus.AVAILABLE -> {
                    if (state.canManageBays) {
                        if (state.showMaintenanceInput) {
                            AtelierButton(
                                text = stringResource(R.string.bays_maintenance_confirm_btn),
                                onClick = onConfirmMaintenance,
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.DestructiveFilled,
                                enabled = state.isOnline && !busy && state.maintenanceReason.isNotBlank(),
                                loading = state.isUpdatingBay
                            )
                            AtelierButton(
                                text = stringResource(R.string.common_back),
                                onClick = { onToggleMaintenanceInput(false) },
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.Text,
                                enabled = !busy
                            )
                        } else {
                            AtelierButton(
                                text = stringResource(R.string.bays_set_maintenance_btn),
                                onClick = { onToggleMaintenanceInput(true) },
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.Outlined,
                                enabled = state.isOnline && !busy,
                                icon = Icons.Outlined.Build
                            )
                        }
                    }
                }
                BayStatus.MAINTENANCE -> {
                    if (state.canManageBays) {
                        AtelierButton(
                            text = stringResource(R.string.bays_restore_btn),
                            onClick = onRestoreBay,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.isOnline && !busy,
                            loading = state.isUpdatingBay
                        )
                    }
                }
                null -> Unit
            }
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !busy
            )
        }
    ) {
        Text(
            text = bay.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        bay.type?.let { type ->
            Text(
                text = stringResource(type.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        if (occupant != null) {
            occupant.plate?.let { AtelierPlate(it, size = PlateSize.L) }
            val elapsed = liveElapsed ?: occupant.laborElapsed
            val mechanicNames = state.mechanicLabelsFor(occupant)
            val orderText = listOfNotNull(
                occupant.internalNumber?.let { stringResource(R.string.today_order_number, it) },
                elapsed?.let { stringResource(R.string.bays_labor, it.formatBayElapsed()) },
                mechanicNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ).joinToString(" · ")
            if (orderText.isNotEmpty()) {
                Text(
                    text = orderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            if (occupant.tasks.isNotEmpty()) {
                AtelierSectionHeader(title = stringResource(R.string.bays_mechanics_section))
                occupant.tasks.forEach { task ->
                    val taskTitle = task.serviceName?.takeIf { it.isNotBlank() } ?: task.description
                    val currentMechanic = state.mechanicNameFor(task.mechanicId, task.mechanicName)
                        ?: stringResource(R.string.order_mechanic_unassigned)
                    val canReassignThisTask = state.canReleaseBay && state.isOnline && !busy &&
                        state.availableMechanics.isNotEmpty()
                    val isReassigningThis = state.reassigningTaskId == task.id.value
                    AtelierListRow(
                        title = taskTitle,
                        subtitle = currentMechanic,
                        onClick = if (canReassignThisTask) {
                            { onOpenReassignTaskMechanic(if (isReassigningThis) null else task.id.value) }
                        } else null,
                        trailing = if (canReassignThisTask) {
                            {
                                AtelierChip(
                                    text = stringResource(R.string.order_reassign_mechanic_btn),
                                    kind = ChipKind.Info
                                )
                            }
                        } else null
                    )
                    if (isReassigningThis) {
                        state.availableMechanics.forEach { mechanic ->
                            AtelierSelectionCard(
                                title = mechanic.displayName,
                                subtitle = mechanic.jobTitle ?: stringResource(R.string.order_mechanic_role_default),
                                selected = mechanic.membershipId == task.mechanicId,
                                enabled = state.isOnline && !busy,
                                onClick = { onConfirmReassignTaskMechanic(task.id.value, mechanic.membershipId) }
                            )
                        }
                    }
                }
            }
            if (state.canAssignBay && state.availableBays.isNotEmpty()) {
                AtelierSectionHeader(title = stringResource(R.string.bays_reassign_section))
                state.availableBays.forEach { targetBay ->
                    AtelierSelectionCard(
                        title = targetBay.name,
                        subtitle = targetBay.type?.let { stringResource(it.labelRes()) },
                        enabled = state.isOnline && !busy,
                        onClick = { onReassignOrder(targetBay) }
                    )
                }
            }
        }
        if (bay.status == BayStatus.AVAILABLE) {
            if (state.showMaintenanceInput) {
                AtelierTextField(
                    label = stringResource(R.string.bays_maintenance_reason_label),
                    value = state.maintenanceReason,
                    onValueChange = onMaintenanceReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.bays_maintenance_reason_hint),
                    enabled = !busy,
                    isError = state.invalidField == "reason",
                    errorText = if (state.invalidField == "reason") {
                        stringResource(R.string.bays_maintenance_field_error)
                    } else null
                )
            } else if (state.canAssignBay && state.assignableOrders.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.bays_select_order_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                state.assignableOrders.forEach { order ->
                    val code = order.internalNumber?.let { stringResource(R.string.today_order_number, it) }
                        ?: stringResource(R.string.today_order_no_number)
                    AtelierListRow(
                        title = order.customerName ?: code,
                        subtitle = code.takeIf { order.customerName != null },
                        plate = order.plate,
                        onClick = if (state.isOnline && !busy) ({ onAssignOrder(order.id) }) else null,
                        trailing = {
                            AtelierChip(
                                text = stringResource(order.status.labelRes()),
                                kind = order.status.chipKind()
                            )
                        }
                    )
                }
            }
        }
        state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/** Elige qué orden entra a la bahía libre. Reasignar es en línea. */
@Composable
internal fun AssignOrderSheet(
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
            AtelierListRow(
                title = order.customerName ?: code,
                subtitle = code.takeIf { order.customerName != null },
                plate = order.plate,
                onClick = if (state.isOnline && !state.isAssigning) ({ onAssign(order.id) }) else null,
                trailing = {
                    AtelierChip(
                        text = stringResource(order.status.labelRes()),
                        kind = order.status.chipKind()
                    )
                }
            )
        }
    }
}

/** Registra una bahía nueva (`BAYS_MANAGE`). En línea. */
@Composable
internal fun CreateBaySheet(
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

/** `1 h 20 min` / `35 min`: el tiempo de mano de obra de las tareas en curso. */
internal fun Duration.formatBayElapsed(): String {
    val minutes = toMinutes().coerceAtLeast(0)
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}
