package com.andeva.atelier.mobile.features.main.presentation.obd

import android.content.res.Configuration
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
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
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
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlanLockedState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

/**
 * Inventario de escáneres OBD-II del taller: cuál está activo, cuál averiado y a qué vehículo está vinculado.
 * Tocar un escáner abre sus acciones (vincular, desvincular, cambiar estado); todas son en línea y van por
 * hoja inferior. Sin telemetría en el plan, la pantalla lo dice en vez de listar.
 */
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = state.devices.firstOrNull { it.id == selectedId }
    val canManage = state.canManageInstallations || state.canRegisterDevices
    // Mientras hay una hoja de la VM abierta, el error se ve dentro de ella y no detrás.
    val sheetOpen = state.registerForm != null || state.editingDevice != null ||
        state.installingDevice != null || state.uninstallingDevice != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onBack != null) {
                AtelierTopBar(title = stringResource(R.string.main_obd_title), onBack = onBack)
            } else {
                AtelierMainTopBar(title = stringResource(R.string.main_obd_title))
            }
        },
        floatingActionButton = {
            if (state.canRegisterDevices && state.planIncludesTelemetry && state.isOnline) {
                AtelierFab(
                    text = stringResource(R.string.obd_register_btn),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenRegister
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.planIncludesTelemetry -> AtelierPlanLockedState(
                    text = stringResource(R.string.obd_plan_locked),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                !state.canReadDevices -> AtelierNoPermissionState(
                    text = stringResource(R.string.obd_no_permission),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                else -> {
                    DeviceFilters(state.statusFilter, onFilterChange)
                    PullToRefreshBox(
                        isRefreshing = state.isLoading && state.devices.isNotEmpty(),
                        onRefresh = { onFilterChange(state.statusFilter) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            // Deja libre el alto del FAB para que la última fila no quede debajo.
                            contentPadding = PaddingValues(bottom = 96.dp + LocalFloatingBarInset.current)
                        ) {
                            if (!state.isOnline) {
                                item(key = "offline") {
                                    AtelierBanner(
                                        message = stringResource(R.string.obd_offline),
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
                            if (error != null && state.devices.isNotEmpty() && !sheetOpen) {
                                item(key = "error") {
                                    AtelierBanner(
                                        message = error.asString(),
                                        icon = Icons.Outlined.ErrorOutline,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                    )
                                }
                            }
                            when {
                                state.isLoading && state.devices.isEmpty() -> items(4, key = { "bone-$it" }) {
                                    AtelierListRow(title = "AA:BB:CC:11:22:33", subtitle = "Bluetooth · ELM327", plate = "ABC123", loading = true)
                                }
                                state.devices.isEmpty() && error != null && !state.isOnline -> item(key = "offline-state") {
                                    AtelierOfflineState(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        onRetry = { onFilterChange(state.statusFilter) }
                                    )
                                }
                                state.devices.isEmpty() && error != null -> item(key = "error-state") {
                                    AtelierErrorState(
                                        message = error.asString(),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        onRetry = { onFilterChange(state.statusFilter) }
                                    )
                                }
                                state.devices.isEmpty() -> item(key = "empty") {
                                    AtelierEmptyState(
                                        text = stringResource(R.string.obd_empty_list),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                else -> items(state.devices, key = { it.id }) { device ->
                                    DeviceRow(
                                        device = device,
                                        installation = state.deviceInstallations[device.id],
                                        onClick = if (canManage) ({ selectedId = device.id }) else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { device ->
        val installation = state.deviceInstallations[device.id]
        DeviceActionsSheet(
            device = device,
            installation = installation,
            canInstall = state.canManageInstallations,
            canChangeStatus = state.canRegisterDevices,
            isOnline = state.isOnline,
            onDismiss = { selectedId = null },
            onChangeStatus = {
                selectedId = null
                onOpenStatusDialog(device)
            },
            onInstall = {
                selectedId = null
                onOpenInstall(device)
            },
            onUninstall = {
                selectedId = null
                onOpenUninstall(device)
            }
        )
    }

    state.registerForm?.let { form ->
        RegisterSheet(
            form = form,
            state = state,
            onDismiss = onDismissRegister,
            onIdentifierChange = onIdentifierChange,
            onConnectionTypeChange = onConnectionTypeChange,
            onHardwareModelChange = onHardwareModelChange,
            onFirmwareVersionChange = onFirmwareVersionChange,
            onConfirm = onConfirmRegister
        )
    }

    state.editingDevice?.let { device ->
        AtelierBottomSheet(title = device.deviceIdentifier, onDismiss = onDismissStatusDialog) {
            Text(
                text = stringResource(R.string.obd_change_status_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.isOnline) AtelierRequiresConnection()
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
            DeviceStatus.entries.forEach { status ->
                AtelierSelectionCard(
                    title = status.label(),
                    selected = device.status == status,
                    enabled = state.isOnline && !state.isSaving,
                    onClick = { onUpdateStatus(status) }
                )
            }
        }
    }

    state.installingDevice?.let { device ->
        AtelierBottomSheet(title = stringResource(R.string.obd_install_dialog_title), onDismiss = onDismissInstall) {
            Text(
                text = stringResource(R.string.obd_install_select_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.isOnline) AtelierRequiresConnection()
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
            if (state.workshopVehicles.isEmpty()) {
                AtelierEmptyState(text = stringResource(R.string.obd_no_vehicles))
            } else {
                state.workshopVehicles.take(INSTALL_VEHICLES_PREVIEW).forEach { item ->
                    val alreadyInstalled = state.deviceInstallations.values.any { it.vehicleId == item.vehicle.id }
                    AtelierListRow(
                        title = "${item.vehicle.brand} ${item.vehicle.model}".trim(),
                        subtitle = if (alreadyInstalled) stringResource(R.string.obd_vehicle_has_scanner)
                        else item.owner?.displayName,
                        plate = item.vehicle.plate,
                        onClick = if (alreadyInstalled || !state.isOnline || state.isSaving) null
                        else ({ onConfirmInstall(item) })
                    )
                }
            }
        }
    }

    state.uninstallingDevice?.let { device ->
        val installInfo = state.deviceInstallations[device.id]
        AtelierBottomSheet(
            title = stringResource(R.string.obd_uninstall_confirm_title),
            onDismiss = onDismissUninstall,
            actions = {
                if (!state.isOnline) AtelierRequiresConnection()
                AtelierButton(
                    text = stringResource(R.string.obd_uninstall_btn),
                    onClick = onConfirmUninstall,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.DestructiveFilled,
                    enabled = state.isOnline && !state.isSaving,
                    loading = state.isSaving
                )
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissUninstall,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isSaving
                )
            }
        ) {
            Text(
                text = stringResource(R.string.obd_uninstall_confirm_prompt, installInfo?.plate ?: device.deviceIdentifier),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
    }
}

/** Cuántos vehículos del taller se ofrecen al vincular (los más recientes del padrón en caché). */
private const val INSTALL_VEHICLES_PREVIEW = 6

@Composable
private fun DeviceFilters(selected: DeviceStatus?, onFilterChange: (DeviceStatus?) -> Unit) {
    val filters = listOf(
        null to stringResource(R.string.obd_filter_all),
        DeviceStatus.ACTIVE to stringResource(R.string.obd_filter_active),
        DeviceStatus.INACTIVE to stringResource(R.string.obd_filter_inactive)
    )
    PrimaryTabRow(
        selectedTabIndex = filters.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        filters.forEach { (status, label) ->
            Tab(
                selected = selected == status,
                onClick = { onFilterChange(status) },
                text = { Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun DeviceRow(device: ObdDevice, installation: DeviceInstallationInfo?, onClick: (() -> Unit)?) {
    val details = listOfNotNull(
        device.connectionType,
        device.hardwareModel,
        device.firmwareVersion?.let { stringResource(R.string.obd_firmware_format, it) }
    ).joinToString(" · ")
    val vehicle = if (installation != null) {
        listOfNotNull(installation.vehicleModel.ifBlank { null }, installation.ownerName).joinToString(" · ")
    } else {
        stringResource(R.string.obd_not_connected)
    }
    AtelierListRow(
        title = device.deviceIdentifier,
        subtitle = listOf(vehicle, details).filter { it.isNotBlank() }.joinToString(" · "),
        plate = installation?.plate,
        onClick = onClick,
        trailing = { device.status?.let { StatusChip(it) } }
    )
}

@Composable
private fun StatusChip(status: DeviceStatus) {
    AtelierChip(
        text = status.label(),
        kind = when (status) {
            DeviceStatus.ACTIVE -> ChipKind.Ok
            DeviceStatus.INACTIVE -> ChipKind.Idle
            DeviceStatus.LOST, DeviceStatus.BROKEN -> ChipKind.Bad
        }
    )
}

/** Acciones de un escáner según permiso: vincular o desvincular, y cambiar su estado. */
@Composable
private fun DeviceActionsSheet(
    device: ObdDevice,
    installation: DeviceInstallationInfo?,
    canInstall: Boolean,
    canChangeStatus: Boolean,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onChangeStatus: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    AtelierBottomSheet(
        title = device.deviceIdentifier,
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            if (canInstall) {
                if (installation != null) {
                    AtelierButton(
                        text = stringResource(R.string.obd_uninstall_btn),
                        onClick = onUninstall,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Destructive,
                        enabled = isOnline,
                        icon = Icons.Outlined.LinkOff
                    )
                } else {
                    AtelierButton(
                        text = stringResource(R.string.obd_install_btn),
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isOnline,
                        icon = Icons.Outlined.Link
                    )
                }
            }
            if (canChangeStatus) {
                AtelierButton(
                    text = stringResource(R.string.obd_change_status_btn),
                    onClick = onChangeStatus,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    enabled = isOnline
                )
            }
        }
    ) {
        device.status?.let { StatusChip(it) }
        val details = listOfNotNull(
            device.connectionType,
            device.hardwareModel,
            device.firmwareVersion?.let { stringResource(R.string.obd_firmware_format, it) }
        ).joinToString(" · ")
        if (details.isNotBlank()) {
            Text(text = details, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
        }
        Text(
            text = installation?.let {
                listOfNotNull(it.plate, it.vehicleModel.ifBlank { null }).joinToString(" · ")
            } ?: stringResource(R.string.obd_not_connected),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        installation?.ownerName?.let {
            Text(
                text = stringResource(R.string.obd_connected_owner_format, it),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

@Composable
private fun RegisterSheet(
    form: RegisterObdForm,
    state: ObdUiState,
    onDismiss: () -> Unit,
    onIdentifierChange: (String) -> Unit,
    onConnectionTypeChange: (String) -> Unit,
    onHardwareModelChange: (String) -> Unit,
    onFirmwareVersionChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.obd_register_dialog_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.obd_register_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.canSubmit && state.isOnline && !state.isSaving,
                loading = state.isSaving
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.obd_field_identifier),
            value = form.deviceIdentifier,
            onValueChange = onIdentifierChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.obd_hint_identifier),
            enabled = !state.isSaving
        )
        Text(
            text = stringResource(R.string.obd_field_connection_type),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        listOf(
            "BLUETOOTH" to stringResource(R.string.obd_conn_bluetooth),
            "WIFI" to stringResource(R.string.obd_conn_wifi),
            "USB" to stringResource(R.string.obd_conn_usb)
        ).forEach { (key, label) ->
            AtelierSelectionCard(
                title = label,
                selected = form.connectionType == key,
                enabled = !state.isSaving,
                onClick = { onConnectionTypeChange(key) }
            )
        }
        AtelierTextField(
            label = stringResource(R.string.obd_field_model),
            value = form.hardwareModel,
            onValueChange = onHardwareModelChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.obd_hint_model),
            enabled = !state.isSaving
        )
        AtelierTextField(
            label = stringResource(R.string.obd_field_firmware),
            value = form.firmwareVersion,
            onValueChange = onFirmwareVersionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.obd_hint_firmware),
            enabled = !state.isSaving
        )
        state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun DeviceStatus.label(): String = when (this) {
    DeviceStatus.ACTIVE -> stringResource(R.string.obd_status_active)
    DeviceStatus.INACTIVE -> stringResource(R.string.obd_status_inactive)
    DeviceStatus.LOST -> stringResource(R.string.obd_status_lost)
    DeviceStatus.BROKEN -> stringResource(R.string.obd_status_broken)
}

// region Previews

private val previewDevices = listOf(
    ObdDevice("d1", "AA:BB:CC:11:22:33", "BLUETOOTH", DeviceStatus.ACTIVE, "OBDLink MX+", "2.1"),
    ObdDevice("d2", "AA:BB:CC:44:55:66", "BLUETOOTH", DeviceStatus.ACTIVE, "ELM327 v1.5", null),
    ObdDevice("d3", "SN-00918", "USB", DeviceStatus.BROKEN, null, null)
)

private val previewState = ObdUiState(
    devices = previewDevices,
    isLoading = false,
    deviceInstallations = mapOf(
        "d1" to DeviceInstallationInfo("i1", "d1", "v1", "BKR418", "Toyota Hilux", "Rosa Quispe Mamani")
    ),
    canReadDevices = true,
    canRegisterDevices = true,
    canManageInstallations = true
)

@Composable
private fun PreviewContent(state: ObdUiState) {
    ObdContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "OBD · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ObdPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "OBD · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObdPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "OBD · plan sin telemetría", widthDp = 390, heightDp = 844)
@Composable
private fun ObdPreviewPlanLocked() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(ObdUiState(planIncludesTelemetry = false, canReadDevices = true, isLoading = false))
}

@Preview(name = "OBD · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun ObdPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(ObdUiState(canReadDevices = true))
}

// endregion
