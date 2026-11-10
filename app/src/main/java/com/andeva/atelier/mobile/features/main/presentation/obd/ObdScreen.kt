package com.andeva.atelier.mobile.features.main.presentation.obd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.designsystem.theme.PillShape
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

@Composable
fun ObdScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ObdViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObdContent(
        state = state,
        onBack = onBack,
        onFilterChange = viewModel::loadDevices,
        onOpenRegister = viewModel::onOpenRegister,
        onDismissRegister = viewModel::onDismissRegister,
        onIdentifierChange = viewModel::onIdentifierChange,
        onConnectionTypeChange = viewModel::onConnectionTypeChange,
        onHardwareModelChange = viewModel::onHardwareModelChange,
        onFirmwareVersionChange = viewModel::onFirmwareVersionChange,
        onConfirmRegister = viewModel::onConfirmRegister,
        onOpenStatusDialog = viewModel::onOpenStatusDialog,
        onDismissStatusDialog = viewModel::onDismissStatusDialog,
        onUpdateStatus = viewModel::onUpdateStatus,
        onOpenInstall = viewModel::onOpenInstall,
        onDismissInstall = viewModel::onDismissInstall,
        onConfirmInstall = viewModel::onConfirmInstall,
        onOpenUninstall = viewModel::onOpenUninstall,
        onDismissUninstall = viewModel::onDismissUninstall,
        onConfirmUninstall = viewModel::onConfirmUninstall,
        modifier = modifier
    )
}

@Composable
internal fun ObdContent(
    state: ObdUiState,
    onBack: (() -> Unit)?,
    onFilterChange: (DeviceStatus?) -> Unit,
    onOpenRegister: () -> Unit,
    onDismissRegister: () -> Unit,
    onIdentifierChange: (String) -> Unit,
    onConnectionTypeChange: (String) -> Unit,
    onHardwareModelChange: (String) -> Unit,
    onFirmwareVersionChange: (String) -> Unit,
    onConfirmRegister: () -> Unit,
    onOpenStatusDialog: (ObdDevice) -> Unit,
    onDismissStatusDialog: () -> Unit,
    onUpdateStatus: (DeviceStatus) -> Unit,
    onOpenInstall: (ObdDevice) -> Unit = {},
    onDismissInstall: () -> Unit = {},
    onConfirmInstall: (VehicleWithOwner) -> Unit = {},
    onOpenUninstall: (ObdDevice) -> Unit = {},
    onDismissUninstall: () -> Unit = {},
    onConfirmUninstall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        if (onBack != null) {
            AtelierTopBar(title = stringResource(R.string.main_obd_title), onBack = onBack)
        } else {
            Text(
                text = stringResource(R.string.main_obd_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.obd_inventory_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )

            if (state.canRegisterDevices) {
                AtelierButton(
                    text = stringResource(R.string.obd_register_btn),
                    onClick = onOpenRegister,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.notice?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
            }
            state.error?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Filled.Error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AtelierButton(
                    text = stringResource(R.string.obd_filter_all),
                    onClick = { onFilterChange(null) },
                    modifier = Modifier.weight(1f),
                    style = if (state.statusFilter == null) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined
                )
                AtelierButton(
                    text = stringResource(R.string.obd_filter_active),
                    onClick = { onFilterChange(DeviceStatus.ACTIVE) },
                    modifier = Modifier.weight(1f),
                    style = if (state.statusFilter == DeviceStatus.ACTIVE) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined
                )
                AtelierButton(
                    text = stringResource(R.string.obd_filter_inactive),
                    onClick = { onFilterChange(DeviceStatus.INACTIVE) },
                    modifier = Modifier.weight(1f),
                    style = if (state.statusFilter == DeviceStatus.INACTIVE) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined
                )
            }

            when {
                state.isLoading -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                state.devices.isEmpty() -> AtelierBanner(
                    message = stringResource(R.string.obd_empty_list),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
                else -> state.devices.forEach { device ->
                    ObdDeviceCard(
                        device = device,
                        installation = state.deviceInstallations[device.id],
                        canManage = state.canManageInstallations || state.canRegisterDevices,
                        onChangeStatus = { onOpenStatusDialog(device) },
                        onInstall = { onOpenInstall(device) },
                        onUninstall = { onOpenUninstall(device) }
                    )
                }
            }

            AtelierButton(
                text = stringResource(R.string.bays_refresh),
                onClick = { onFilterChange(state.statusFilter) },
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                loading = state.isLoading
            )
        }
    }

    state.registerForm?.let { form ->
        AlertDialog(
            onDismissRequest = onDismissRegister,
            confirmButton = {},
            title = {
                Text(
                    text = stringResource(R.string.obd_register_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AtelierTextField(
                        label = stringResource(R.string.obd_field_identifier),
                        value = form.deviceIdentifier,
                        onValueChange = onIdentifierChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Ej: AA:BB:CC:11:22:33"
                    )

                    // Selector de tipo de conexión segmented moderno (Lemon Cash style)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.obd_field_connection_type),
                            style = MaterialTheme.typography.labelMedium,
                            color = AtelierTheme.colors.muted
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, AtelierTheme.colors.outlineSoft.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val options = listOf(
                                    "BLUETOOTH" to "Bluetooth",
                                    "WIFI" to "Wi-Fi",
                                    "USB" to "USB"
                                )
                                options.forEach { (key, label) ->
                                    val isSelected = form.connectionType == key
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = !state.isSaving) { onConnectionTypeChange(key) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shadowElevation = if (isSelected) 2.dp else 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AtelierTextField(
                        label = stringResource(R.string.obd_field_model),
                        value = form.hardwareModel,
                        onValueChange = onHardwareModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Ej: ELM327 v1.5 / OBDLink MX+"
                    )
                    AtelierTextField(
                        label = stringResource(R.string.obd_field_firmware),
                        value = form.firmwareVersion,
                        onValueChange = onFirmwareVersionChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Ej: v2.1"
                    )

                    Spacer(Modifier.height(4.dp))

                    AtelierButton(
                        text = stringResource(R.string.obd_register_confirm),
                        onClick = onConfirmRegister,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = form.canSubmit && !state.isSaving,
                        loading = state.isSaving
                    )
                    AtelierButton(
                        text = stringResource(R.string.proposals_cancel),
                        onClick = onDismissRegister,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !state.isSaving
                    )
                }
            }
        )
    }

    state.editingDevice?.let { device ->
        AlertDialog(
            onDismissRequest = onDismissStatusDialog,
            confirmButton = {},
            title = { Text(device.deviceIdentifier) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.obd_change_status_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    DeviceStatus.entries.forEach { status ->
                        AtelierButton(
                            text = status.label(),
                            onClick = { onUpdateStatus(status) },
                            modifier = Modifier.fillMaxWidth(),
                            style = if (device.status == status) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                            enabled = !state.isSaving,
                            loading = state.isSaving
                        )
                    }
                    AtelierButton(
                        text = stringResource(R.string.proposals_cancel),
                        onClick = onDismissStatusDialog,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !state.isSaving
                    )
                }
            }
        )
    }

    state.installingDevice?.let { device ->
        AlertDialog(
            onDismissRequest = onDismissInstall,
            confirmButton = {},
            title = { Text(stringResource(R.string.obd_install_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.obd_install_select_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    if (state.workshopVehicles.isEmpty()) {
                        Text(
                            text = "No hay vehículos cargados en el taller.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtelierTheme.colors.muted
                        )
                    } else {
                        state.workshopVehicles.take(6).forEach { item ->
                            val alreadyInstalled = state.deviceInstallations.values.any { it.vehicleId == item.vehicle.id }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CardShape)
                                    .clickable(enabled = !alreadyInstalled && !state.isSaving) {
                                        onConfirmInstall(item)
                                    }
                                    .background(
                                        if (alreadyInstalled) AtelierTheme.colors.idleContainer.copy(alpha = 0.35f)
                                        else AtelierTheme.colors.card
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${item.vehicle.plate} · ${item.vehicle.brand} ${item.vehicle.model}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (alreadyInstalled) AtelierTheme.colors.muted else MaterialTheme.colorScheme.onSurface
                                    )
                                    item.owner?.displayName?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AtelierTheme.colors.inkSecondary
                                        )
                                    }
                                    if (alreadyInstalled) {
                                        Text(
                                            text = "Ya cuenta con escáner instalado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AtelierTheme.colors.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                    AtelierButton(
                        text = stringResource(R.string.proposals_cancel),
                        onClick = onDismissInstall,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !state.isSaving
                    )
                }
            }
        )
    }

    state.uninstallingDevice?.let { device ->
        val installInfo = state.deviceInstallations[device.id]
        AlertDialog(
            onDismissRequest = onDismissUninstall,
            confirmButton = {},
            title = { Text(stringResource(R.string.obd_uninstall_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.obd_uninstall_confirm_prompt,
                            installInfo?.plate ?: device.deviceIdentifier
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AtelierButton(
                        text = stringResource(R.string.obd_uninstall_btn),
                        onClick = onConfirmUninstall,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        loading = state.isSaving
                    )
                    AtelierButton(
                        text = stringResource(R.string.proposals_cancel),
                        onClick = onDismissUninstall,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !state.isSaving
                    )
                }
            }
        )
    }
}

@Composable
private fun ObdDeviceCard(
    device: ObdDevice,
    installation: DeviceInstallationInfo?,
    canManage: Boolean,
    onChangeStatus: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(atelier.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = device.deviceIdentifier,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            device.status?.let { status ->
                AtelierChip(
                    text = status.label(),
                    kind = when (status) {
                        DeviceStatus.ACTIVE -> ChipKind.Ok
                        DeviceStatus.INACTIVE -> ChipKind.Idle
                        DeviceStatus.LOST, DeviceStatus.BROKEN -> ChipKind.Bad
                    }
                )
            }
        }

        val details = listOfNotNull(
            device.connectionType,
            device.hardwareModel,
            device.firmwareVersion?.let { "FW $it" }
        ).joinToString(" · ")
        if (details.isNotBlank()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = atelier.inkSecondary
            )
        }

        // Estado de conexión a vehículo (sin emojis, usando icono vector Material y tarjeta limpia)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (installation != null) scheme.primaryContainer.copy(alpha = 0.25f)
            else atelier.idleContainer.copy(alpha = 0.45f),
            border = BorderStroke(
                width = 1.dp,
                color = if (installation != null) scheme.primary.copy(alpha = 0.25f)
                else atelier.outlineSoft.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (installation != null) scheme.primary.copy(alpha = 0.15f)
                    else scheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (installation != null) Icons.Outlined.DirectionsCar else Icons.Outlined.LinkOff,
                            contentDescription = null,
                            tint = if (installation != null) scheme.primary else atelier.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (installation != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = installation.plate,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary
                            )
                            Text(
                                text = "· ${installation.vehicleModel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        installation.ownerName?.let { owner ->
                            Text(
                                text = stringResource(R.string.obd_connected_owner_format, owner),
                                style = MaterialTheme.typography.labelSmall,
                                color = atelier.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.obd_not_connected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = atelier.muted
                        )
                    }
                }
            }
        }

        if (canManage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtelierButton(
                    text = stringResource(R.string.obd_change_status_btn),
                    onClick = onChangeStatus,
                    style = AtelierButtonStyle.Text
                )
                if (installation != null) {
                    AtelierButton(
                        text = stringResource(R.string.obd_uninstall_btn),
                        onClick = onUninstall,
                        style = AtelierButtonStyle.Outlined,
                        icon = Icons.Outlined.LinkOff
                    )
                } else {
                    AtelierButton(
                        text = stringResource(R.string.obd_install_btn),
                        onClick = onInstall,
                        style = AtelierButtonStyle.Tonal,
                        icon = Icons.Outlined.Link
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceStatus.label(): String = when (this) {
    DeviceStatus.ACTIVE -> stringResource(R.string.obd_status_active)
    DeviceStatus.INACTIVE -> stringResource(R.string.obd_status_inactive)
    DeviceStatus.LOST -> stringResource(R.string.obd_status_lost)
    DeviceStatus.BROKEN -> stringResource(R.string.obd_status_broken)
}
