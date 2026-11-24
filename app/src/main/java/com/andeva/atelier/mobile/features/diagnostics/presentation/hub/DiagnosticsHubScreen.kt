package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlanLockedState
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
import com.andeva.atelier.mobile.core.designsystem.component.LocalAtelierMinTouchTarget
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapter
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertType
import com.andeva.atelier.mobile.features.diagnostics.presentation.chipKind
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.labelRes
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun DiagnosticsHubScreen(
    onBack: (() -> Unit)?,
    onOpenLiveTelemetry: (vehicleId: String, plate: String?) -> Unit,
    onOpenVehicleFaults: (vehicleId: String, plate: String?) -> Unit,
    onOpenAiHealthReport: (vehicleId: String, plate: String?) -> Unit,
    onReportFinding: (workOrderId: String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSyncQueue: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsHubViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onBluetoothPermissionResult
    )

    DiagnosticsHubContent(
        state = state,
        onBack = onBack,
        onSelectTab = viewModel::selectTab,
        onOpenVehiclePicker = viewModel::openVehiclePicker,
        onDismissVehiclePicker = viewModel::dismissVehiclePicker,
        onVehicleSearchQueryChange = viewModel::onVehicleSearchQueryChange,
        onSelectVehicle = viewModel::selectVehicle,
        onOpenInstallForVehicle = viewModel::openInstallForSelectedVehicle,
        onDismissInstallForVehicle = viewModel::dismissInstallForSelectedVehicle,
        onInstallOdometerChange = viewModel::onInstallOdometerChange,
        onConfirmInstallOnVehicle = viewModel::confirmInstallOnSelectedVehicle,
        onUninstallFromVehicle = viewModel::uninstallFromSelectedVehicle,
        onRequestBluetoothPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                viewModel.loadPairedAdapters()
            }
        },
        onRefreshAdapters = viewModel::loadPairedAdapters,
        onSelectAdapter = viewModel::selectAdapter,
        onConnectAdapter = viewModel::connectSelectedAdapter,
        onDisconnectAdapter = viewModel::disconnectAdapter,
        onOpenLiveTelemetry = onOpenLiveTelemetry,
        onOpenVehicleFaults = onOpenVehicleFaults,
        onOpenAiHealthReport = onOpenAiHealthReport,
        onReportFinding = onReportFinding,
        onFilterDevices = viewModel::loadDevices,
        onOpenRegisterDevice = viewModel::onOpenRegisterDevice,
        onDismissRegisterDevice = viewModel::onDismissRegisterDevice,
        onIdentifierChange = viewModel::onIdentifierChange,
        onConnectionTypeChange = viewModel::onConnectionTypeChange,
        onHardwareModelChange = viewModel::onHardwareModelChange,
        onFirmwareVersionChange = viewModel::onFirmwareVersionChange,
        onConfirmRegisterDevice = viewModel::onConfirmRegisterDevice,
        onOpenStatusSheet = viewModel::onOpenStatusSheet,
        onDismissStatusSheet = viewModel::onDismissStatusSheet,
        onUpdateDeviceStatus = viewModel::onUpdateDeviceStatus,
        onOpenInstallDevice = viewModel::onOpenInstallDevice,
        onDismissInstallDevice = viewModel::onDismissInstallDevice,
        onConfirmInstallDevice = viewModel::onConfirmInstallDevice,
        onOpenUninstallDevice = viewModel::onOpenUninstallDevice,
        onDismissUninstallDevice = viewModel::onDismissUninstallDevice,
        onConfirmUninstallDevice = viewModel::onConfirmUninstallDevice,
        onSetAlertsScope = viewModel::setAlertsScope,
        onRefreshAlerts = viewModel::refreshAlerts,
        onAcknowledgeAlert = viewModel::acknowledgeAlert,
        onOpenConvertAlert = viewModel::openConvertAlertSheet,
        onDismissConvertAlert = viewModel::dismissConvertAlertSheet,
        onConvertAlertQuickDate = viewModel::onConvertAlertQuickDate,
        onConvertAlertDateChange = viewModel::onConvertAlertDateChange,
        onConvertAlertTimeChange = viewModel::onConvertAlertTimeChange,
        onConvertAlertDurationChange = viewModel::onConvertAlertDurationChange,
        onConvertAlertReasonChange = viewModel::onConvertAlertReasonChange,
        onConfirmConvertAlert = viewModel::confirmConvertAlertToAppointment,
        onOpenNotifications = onOpenNotifications,
        onOpenSyncQueue = onOpenSyncQueue,
        onOpenProfile = onOpenProfile,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsHubContent(
    state: DiagnosticsHubUiState,
    onBack: (() -> Unit)?,
    onSelectTab: (DiagnosticsHubTab) -> Unit,
    onOpenVehiclePicker: () -> Unit,
    onDismissVehiclePicker: () -> Unit,
    onVehicleSearchQueryChange: (String) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onOpenInstallForVehicle: () -> Unit,
    onDismissInstallForVehicle: () -> Unit,
    onInstallOdometerChange: (String) -> Unit,
    onConfirmInstallOnVehicle: (ObdDevice) -> Unit,
    onUninstallFromVehicle: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onRefreshAdapters: () -> Unit,
    onSelectAdapter: (String) -> Unit,
    onConnectAdapter: () -> Unit,
    onDisconnectAdapter: () -> Unit,
    onOpenLiveTelemetry: (String, String?) -> Unit,
    onOpenVehicleFaults: (String, String?) -> Unit,
    onOpenAiHealthReport: (String, String?) -> Unit,
    onReportFinding: (String) -> Unit,
    onFilterDevices: (DeviceStatus?) -> Unit,
    onOpenRegisterDevice: () -> Unit,
    onDismissRegisterDevice: () -> Unit,
    onIdentifierChange: (String) -> Unit,
    onConnectionTypeChange: (String) -> Unit,
    onHardwareModelChange: (String) -> Unit,
    onFirmwareVersionChange: (String) -> Unit,
    onConfirmRegisterDevice: () -> Unit,
    onOpenStatusSheet: (ObdDevice) -> Unit,
    onDismissStatusSheet: () -> Unit,
    onUpdateDeviceStatus: (DeviceStatus) -> Unit,
    onOpenInstallDevice: (ObdDevice) -> Unit,
    onDismissInstallDevice: () -> Unit,
    onConfirmInstallDevice: (VehicleWithOwner) -> Unit,
    onOpenUninstallDevice: (ObdDevice) -> Unit,
    onDismissUninstallDevice: () -> Unit,
    onConfirmUninstallDevice: () -> Unit,
    onSetAlertsScope: (AlertsScope) -> Unit,
    onRefreshAlerts: () -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
    onOpenConvertAlert: (PredictiveAlert) -> Unit,
    onDismissConvertAlert: () -> Unit,
    onConfirmConvertAlert: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onConvertAlertQuickDate: (Long) -> Unit = {},
    onConvertAlertDateChange: (String) -> Unit = {},
    onConvertAlertTimeChange: (String) -> Unit = {},
    onConvertAlertDurationChange: (String) -> Unit = {},
    onConvertAlertReasonChange: (String) -> Unit = {}
) = CompositionLocalProvider(LocalAtelierMinTouchTarget provides 56.dp) {
    val haptics = rememberAtelierHaptics()
    var selectedDeviceIdForMenu by remember { mutableStateOf<String?>(null) }
    val selectedDeviceForMenu = state.devices.firstOrNull { it.id == selectedDeviceIdForMenu }
    val sheetOpen = state.vehiclePickerOpen || state.installForVehicleSheetOpen ||
        state.registerForm != null || state.editingStatusDevice != null ||
        state.installingDevice != null || state.uninstallingDevice != null ||
        state.convertingAlert != null || selectedDeviceForMenu != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onBack != null) {
                AtelierTopBar(title = stringResource(R.string.diag_hub_title), onBack = onBack)
            } else {
                AtelierMainTopBar(
                    title = stringResource(R.string.diag_hub_title),
                    onOpenNotifications = onOpenNotifications,
                    unreadNotificationsCount = state.unreadNotificationsCount,
                    onOpenSync = onOpenSyncQueue,
                    pendingSyncCount = state.pendingSyncCount,
                    onOpenProfile = onOpenProfile
                )
            }
        },
        floatingActionButton = {
            if (state.selectedTab == DiagnosticsHubTab.DEVICES &&
                state.planIncludesTelemetry &&
                state.canRegisterDevices &&
                state.isOnline
            ) {
                AtelierFab(
                    text = stringResource(R.string.obd_register_btn),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenRegisterDevice,
                    modifier = Modifier.padding(bottom = LocalFloatingBarInset.current)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.planIncludesTelemetry && !state.planIncludesAi -> AtelierPlanLockedState(
                    text = stringResource(R.string.diag_hub_plan_locked),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                state.availableTabs.isEmpty() && !state.planIncludesTelemetry -> AtelierPlanLockedState(
                    text = stringResource(R.string.diag_hub_plan_locked),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                state.availableTabs.isEmpty() -> AtelierNoPermissionState(
                    text = stringResource(R.string.diag_hub_no_permission),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                else -> {
                    if (state.availableTabs.size > 1) {
                        HubTabs(
                            tabs = state.availableTabs,
                            selected = state.selectedTab,
                            onSelect = onSelectTab
                        )
                    }
                    if (!sheetOpen) {
                        state.notice?.let {
                            AtelierBanner(
                                message = it.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
                            )
                        }
                        state.error?.let {
                            AtelierBanner(
                                message = it.asString(),
                                icon = Icons.Outlined.ErrorOutline,
                                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
                            )
                        }
                    }
                    when (state.selectedTab) {
                        DiagnosticsHubTab.SCANNER -> ScannerTabContent(
                            state = state,
                            onOpenVehiclePicker = onOpenVehiclePicker,
                            onOpenInstallForVehicle = onOpenInstallForVehicle,
                            onUninstallFromVehicle = onUninstallFromVehicle,
                            onRequestBluetoothPermission = onRequestBluetoothPermission,
                            onRefreshAdapters = onRefreshAdapters,
                            onSelectAdapter = onSelectAdapter,
                            onConnectAdapter = {
                                haptics.toggle()
                                onConnectAdapter()
                            },
                            onDisconnectAdapter = {
                                haptics.toggle()
                                onDisconnectAdapter()
                            },
                            onOpenLiveTelemetry = onOpenLiveTelemetry,
                            onOpenVehicleFaults = onOpenVehicleFaults,
                            onOpenAiHealthReport = onOpenAiHealthReport,
                            onReportFinding = onReportFinding
                        )
                        DiagnosticsHubTab.DEVICES -> DevicesTabContent(
                            state = state,
                            onFilterDevices = onFilterDevices,
                            onSelectDevice = { selectedDeviceIdForMenu = it.id }
                        )
                        DiagnosticsHubTab.ALERTS -> AlertsTabContent(
                            state = state,
                            onSetAlertsScope = onSetAlertsScope,
                            onRefreshAlerts = onRefreshAlerts,
                            onAcknowledgeAlert = { alertId ->
                                haptics.confirm()
                                onAcknowledgeAlert(alertId)
                            },
                            onOpenConvertAlert = onOpenConvertAlert
                        )
                    }
                }
            }
        }
    }

    if (state.vehiclePickerOpen) {
        VehiclePickerSheet(
            query = state.vehicleSearchQuery,
            vehicles = state.filteredVehicles,
            selectedVehicleId = state.selectedVehicleId,
            onQueryChange = onVehicleSearchQueryChange,
            onSelectVehicle = onSelectVehicle,
            onDismiss = onDismissVehiclePicker
        )
    }

    if (state.installForVehicleSheetOpen) {
        InstallOnVehicleSheet(
            state = state,
            onOdometerChange = onInstallOdometerChange,
            onSelectDevice = { device ->
                haptics.confirm()
                onConfirmInstallOnVehicle(device)
            },
            onDismiss = onDismissInstallForVehicle
        )
    }

    selectedDeviceForMenu?.let { device ->
        val installation = state.deviceInstallations[device.id]
        DeviceActionsSheet(
            device = device,
            installation = installation,
            canInstall = state.canManageInstallations,
            canChangeStatus = state.canRegisterDevices,
            isOnline = state.isOnline,
            onDismiss = { selectedDeviceIdForMenu = null },
            onChangeStatus = {
                selectedDeviceIdForMenu = null
                onOpenStatusSheet(device)
            },
            onInstall = {
                selectedDeviceIdForMenu = null
                onOpenInstallDevice(device)
            },
            onUninstall = {
                selectedDeviceIdForMenu = null
                onOpenUninstallDevice(device)
            }
        )
    }

    state.registerForm?.let { form ->
        RegisterDeviceSheet(
            form = form,
            isOnline = state.isOnline,
            isSaving = state.isSavingDeviceAction,
            error = state.error,
            onDismiss = onDismissRegisterDevice,
            onIdentifierChange = onIdentifierChange,
            onConnectionTypeChange = onConnectionTypeChange,
            onHardwareModelChange = onHardwareModelChange,
            onFirmwareVersionChange = onFirmwareVersionChange,
            onConfirm = {
                haptics.confirm()
                onConfirmRegisterDevice()
            }
        )
    }

    state.editingStatusDevice?.let { device ->
        AtelierBottomSheet(title = device.deviceIdentifier, onDismiss = onDismissStatusSheet) {
            Text(
                text = stringResource(R.string.obd_change_status_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.isOnline) AtelierRequiresConnection()
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
            DeviceStatus.entries.forEach { status ->
                AtelierSelectionCard(
                    title = stringResource(status.labelRes()),
                    selected = device.status == status,
                    enabled = state.isOnline && !state.isSavingDeviceAction,
                    onClick = {
                        haptics.confirm()
                        onUpdateDeviceStatus(status)
                    }
                )
            }
        }
    }

    state.installingDevice?.let { device ->
        AtelierBottomSheet(title = stringResource(R.string.obd_install_dialog_title), onDismiss = onDismissInstallDevice) {
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
                state.workshopVehicles.take(8).forEach { item ->
                    val alreadyInstalled = state.deviceInstallations.values.any { it.vehicleId == item.vehicle.id }
                    AtelierListRow(
                        title = "${item.vehicle.brand} ${item.vehicle.model}".trim(),
                        subtitle = if (alreadyInstalled) stringResource(R.string.obd_vehicle_has_scanner)
                        else item.owner?.displayName,
                        plate = item.vehicle.plate,
                        onClick = if (alreadyInstalled || !state.isOnline || state.isSavingDeviceAction) null
                        else ({
                            haptics.confirm()
                            onConfirmInstallDevice(item)
                        })
                    )
                }
            }
        }
    }

    state.uninstallingDevice?.let { device ->
        val installInfo = state.deviceInstallations[device.id]
        AtelierBottomSheet(
            title = stringResource(R.string.obd_uninstall_confirm_title),
            onDismiss = onDismissUninstallDevice,
            actions = {
                if (!state.isOnline) AtelierRequiresConnection()
                AtelierButton(
                    text = stringResource(R.string.obd_uninstall_btn),
                    onClick = {
                        haptics.confirm()
                        onConfirmUninstallDevice()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.DestructiveFilled,
                    enabled = state.isOnline && !state.isSavingDeviceAction,
                    loading = state.isSavingDeviceAction
                )
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissUninstallDevice,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isSavingDeviceAction
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

    state.convertingAlert?.let { alert ->
        val plate = state.workshopVehicles.firstOrNull { it.vehicle.id == alert.vehicleId }?.vehicle?.plate
        AtelierBottomSheet(
            title = stringResource(R.string.diag_alerts_convert_title),
            onDismiss = onDismissConvertAlert,
            actions = {
                if (!state.isOnline) AtelierRequiresConnection()
                AtelierButton(
                    text = stringResource(R.string.diag_alerts_convert_confirm),
                    onClick = {
                        haptics.confirm()
                        onConfirmConvertAlert()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isOnline && !state.isActingOnAlert,
                    loading = state.isActingOnAlert,
                    icon = Icons.Outlined.EventAvailable
                )
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissConvertAlert,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isActingOnAlert
                )
            }
        ) {
            if (!plate.isNullOrBlank()) {
                AtelierPlate(plate = plate, size = PlateSize.L)
            }
            Text(
                text = stringResource(alert.alertType.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.diag_alerts_convert_body),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = false,
                    onClick = { onConvertAlertQuickDate(0L) },
                    enabled = !state.isActingOnAlert,
                    label = { Text(stringResource(R.string.diag_alerts_convert_today)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { onConvertAlertQuickDate(1L) },
                    enabled = !state.isActingOnAlert,
                    label = { Text(stringResource(R.string.diag_alerts_convert_tomorrow)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { onConvertAlertQuickDate(2L) },
                    enabled = !state.isActingOnAlert,
                    label = { Text(stringResource(R.string.diag_alerts_convert_in_2_days)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                AtelierTextField(
                    label = stringResource(R.string.diag_alerts_convert_date_label),
                    value = state.convertAlertDateText,
                    onValueChange = onConvertAlertDateChange,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isActingOnAlert,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                AtelierTextField(
                    label = stringResource(R.string.diag_alerts_convert_time_label),
                    value = state.convertAlertTimeText,
                    onValueChange = onConvertAlertTimeChange,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isActingOnAlert,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                listOf("09:00", "11:00", "15:00").forEach { slot ->
                    FilterChip(
                        selected = state.convertAlertTimeText == slot,
                        onClick = { onConvertAlertTimeChange(slot) },
                        enabled = !state.isActingOnAlert,
                        label = { Text(slot) }
                    )
                }
            }
            AtelierTextField(
                label = stringResource(R.string.diag_alerts_convert_duration_label),
                value = state.convertAlertDurationText,
                onValueChange = onConvertAlertDurationChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isActingOnAlert,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            AtelierTextField(
                label = stringResource(R.string.diag_alerts_convert_reason_label),
                value = state.convertAlertReason,
                onValueChange = onConvertAlertReasonChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isActingOnAlert
            )
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
    }
}

@Composable
private fun HubTabs(
    tabs: List<DiagnosticsHubTab>,
    selected: DiagnosticsHubTab,
    onSelect: (DiagnosticsHubTab) -> Unit
) {
    val labels = mapOf(
        DiagnosticsHubTab.SCANNER to stringResource(R.string.diag_tab_scanner),
        DiagnosticsHubTab.DEVICES to stringResource(R.string.diag_tab_devices),
        DiagnosticsHubTab.ALERTS to stringResource(R.string.diag_tab_alerts)
    )
    AtelierSegmentedControl(
        options = tabs,
        selected = selected,
        onSelect = onSelect,
        label = { labels[it].orEmpty() },
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}

// region Tab 1: Escáner en vivo

@Composable
private fun ScannerTabContent(
    state: DiagnosticsHubUiState,
    onOpenVehiclePicker: () -> Unit,
    onOpenInstallForVehicle: () -> Unit,
    onUninstallFromVehicle: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onRefreshAdapters: () -> Unit,
    onSelectAdapter: (String) -> Unit,
    onConnectAdapter: () -> Unit,
    onDisconnectAdapter: () -> Unit,
    onOpenLiveTelemetry: (String, String?) -> Unit,
    onOpenVehicleFaults: (String, String?) -> Unit,
    onOpenAiHealthReport: (String, String?) -> Unit,
    onReportFinding: (String) -> Unit
) {
    val selectedVehicle = state.selectedVehicle
    val showVehicleSkeleton = rememberDelayedLoading(state.vehiclesLoading && state.workshopVehicles.isEmpty())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md,
            bottom = Spacing.lg + LocalFloatingBarInset.current
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "vehicle-section") {
            AtelierSectionHeader(
                title = stringResource(R.string.diag_vehicle_section),
                action = if (state.workshopVehicles.isNotEmpty()) {
                    {
                        AtelierButton(
                            text = if (selectedVehicle == null) stringResource(R.string.diag_vehicle_pick)
                            else stringResource(R.string.diag_vehicle_change),
                            onClick = onOpenVehiclePicker,
                            style = AtelierButtonStyle.Text
                        )
                    }
                } else null
            )
            when {
                showVehicleSkeleton -> {
                    AtelierListRow(
                        title = "Toyota Hilux",
                        subtitle = "Rosa Quispe Mamani",
                        plate = "BKL482",
                        loading = true
                    )
                }
                selectedVehicle == null -> {
                    AtelierEmptyState(
                        text = stringResource(R.string.diag_vehicle_none),
                        actionLabel = if (state.workshopVehicles.isNotEmpty()) stringResource(R.string.diag_vehicle_pick) else null,
                        onAction = if (state.workshopVehicles.isNotEmpty()) onOpenVehiclePicker else null
                    )
                }
                else -> {
                    AtelierSurface(
                        modifier = Modifier.fillMaxWidth(),
                        tier = SurfaceTier.Card,
                        shape = CardLargeShape,
                        contentPadding = PaddingValues(Spacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AtelierPlate(plate = selectedVehicle.vehicle.plate, size = PlateSize.L)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${selectedVehicle.vehicle.brand} ${selectedVehicle.vehicle.model}".trim(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    selectedVehicle.owner?.displayName?.let { owner ->
                                        Text(
                                            text = owner,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AtelierTheme.colors.inkSecondary
                                        )
                                    }
                                }
                            }

                            if (!state.vehicleSyncState.isSynced) {
                                AtelierChip(
                                    text = stringResource(
                                        R.string.diag_live_sync_pending,
                                        state.vehicleSyncState.pendingReadings + state.vehicleSyncState.pendingFaults
                                    ),
                                    kind = ChipKind.Idle
                                )
                            }

                            if (state.planIncludesTelemetry && (state.canReadDevices || state.canManageInstallations)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                InstallationStatusRow(
                                    state = state,
                                    onInstall = onOpenInstallForVehicle,
                                    onUninstall = onUninstallFromVehicle
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.canConnectBluetooth || state.isBluetoothConnected) {
            item(key = "bt-section") {
                BluetoothSection(
                    state = state,
                    onRequestPermission = onRequestBluetoothPermission,
                    onRefreshAdapters = onRefreshAdapters,
                    onSelectAdapter = onSelectAdapter,
                    onConnect = onConnectAdapter,
                    onDisconnect = onDisconnectAdapter
                )
            }
        }

        if (selectedVehicle != null) {
            item(key = "tools-section") {
                val vehicleId = selectedVehicle.vehicle.id
                val plate = selectedVehicle.vehicle.plate
                val workOrderId = state.selectedVehicleWorkOrderId
                AtelierGroupedCard(title = stringResource(R.string.diag_actions_section)) {
                    var anyRow = false
                    if (state.planIncludesTelemetry && (state.canUseTelemetry || state.canReadTelemetry)) {
                        anyRow = true
                        AtelierGroupedRow(
                            title = stringResource(R.string.diag_action_live_title),
                            subtitle = if (state.isBluetoothConnected || state.canUseTelemetry) {
                                stringResource(R.string.diag_action_live_sub_bt)
                            } else {
                                stringResource(R.string.diag_action_live_sub_remote)
                            },
                            icon = Icons.Outlined.Speed,
                            showDivider = state.canReadFaults || state.canUseTelemetry || state.canResolveFaults || state.canUseAiDiagnostics || state.canReadAiReports,
                            onClick = { onOpenLiveTelemetry(vehicleId, plate) }
                        )
                    }
                    if (state.planIncludesTelemetry && (state.canReadFaults || state.canUseTelemetry || state.canResolveFaults)) {
                        anyRow = true
                        AtelierGroupedRow(
                            title = stringResource(R.string.diag_action_faults_title),
                            subtitle = stringResource(R.string.diag_action_faults_sub),
                            icon = Icons.Outlined.Build,
                            showDivider = state.canUseAiDiagnostics || state.canReadAiReports || !state.planIncludesAi ||
                                (state.canReportFinding && workOrderId != null),
                            onClick = { onOpenVehicleFaults(vehicleId, plate) }
                        )
                    }
                    if (state.canUseAiDiagnostics || state.canReadAiReports || !state.planIncludesAi) {
                        anyRow = true
                        AtelierGroupedRow(
                            title = stringResource(R.string.diag_action_ai_title),
                            subtitle = if (state.planIncludesAi) {
                                stringResource(R.string.diag_action_ai_sub)
                            } else {
                                stringResource(R.string.diag_action_ai_locked_sub)
                            },
                            icon = Icons.Outlined.AutoAwesome,
                            showDivider = state.canReportFinding && workOrderId != null,
                            onClick = { onOpenAiHealthReport(vehicleId, plate) }
                        )
                    }
                    if (state.canReportFinding && workOrderId != null) {
                        AtelierGroupedRow(
                            title = stringResource(R.string.diag_action_finding_title),
                            subtitle = stringResource(R.string.diag_action_finding_sub),
                            icon = Icons.Outlined.ReportProblem,
                            showDivider = false,
                            onClick = { onReportFinding(workOrderId) }
                        )
                    } else if (!anyRow) {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.diag_hub_no_permission),
                            modifier = Modifier.padding(Spacing.md)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallationStatusRow(
    state: DiagnosticsHubUiState,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val active = state.activeInstallation
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (active != null) stringResource(R.string.diag_installation_active)
                    else stringResource(R.string.diag_installation_none),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (active != null) stringResource(R.string.diag_installation_active_sub)
                    else stringResource(R.string.diag_installation_none_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            AtelierChip(
                text = if (active != null) stringResource(R.string.obd_status_active)
                else stringResource(R.string.diag_bt_status_disconnected),
                kind = if (active != null) ChipKind.Ok else ChipKind.Idle
            )
        }
        if (!state.isOnline) {
            AtelierBanner(
                message = stringResource(R.string.diag_installation_offline),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Info
            )
        } else if (state.canManageInstallations) {
            if (active == null) {
                AtelierButton(
                    text = stringResource(R.string.diag_installation_install_cta),
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    enabled = !state.isLoadingInstallation && !state.isSavingDeviceAction,
                    loading = state.isLoadingInstallation,
                    icon = Icons.Outlined.Link
                )
            } else {
                AtelierButton(
                    text = stringResource(R.string.diag_installation_uninstall_cta),
                    onClick = onUninstall,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Destructive,
                    enabled = !state.isSavingDeviceAction,
                    loading = state.isSavingDeviceAction,
                    icon = Icons.Outlined.LinkOff
                )
            }
        }
    }
}

@Composable
private fun BluetoothSection(
    state: DiagnosticsHubUiState,
    onRequestPermission: () -> Unit,
    onRefreshAdapters: () -> Unit,
    onSelectAdapter: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        AtelierSectionHeader(
            title = stringResource(R.string.diag_bt_section),
            action = {
                AtelierChip(
                    text = stringResource(state.linkState.labelRes()),
                    kind = state.linkState.chipKind(),
                    icon = if (state.isBluetoothConnected) Icons.Outlined.BluetoothConnected else Icons.Outlined.Bluetooth
                )
            }
        )
        when {
            state.bluetoothPermissionRequired -> {
                AtelierBanner(
                    message = stringResource(R.string.diag_bt_permission_needed),
                    icon = Icons.Outlined.Bluetooth,
                    kind = BannerKind.Wait
                )
                AtelierButton(
                    text = stringResource(R.string.diag_bt_permission_cta),
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Bluetooth
                )
            }
            state.bluetoothDisabled -> {
                AtelierBanner(
                    message = stringResource(R.string.diag_bt_disabled),
                    icon = Icons.Outlined.BluetoothDisabled,
                    kind = BannerKind.Wait
                )
                AtelierButton(
                    text = stringResource(R.string.diag_bt_refresh),
                    onClick = onRefreshAdapters,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    icon = Icons.Outlined.Refresh
                )
            }
            state.isBluetoothConnected -> {
                AtelierSurface(
                    modifier = Modifier.fillMaxWidth(),
                    tier = SurfaceTier.Card,
                    shape = CardLargeShape,
                    contentPadding = PaddingValues(Spacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        state.connectionInfo?.protocol?.let { proto ->
                            Text(
                                text = stringResource(R.string.diag_bt_connected_protocol, proto),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        state.connectionInfo?.vin?.let { vin ->
                            Text(
                                text = stringResource(R.string.diag_bt_connected_vin, vin),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AtelierTheme.colors.inkSecondary
                            )
                        }
                        AtelierButton(
                            text = stringResource(R.string.diag_bt_disconnect),
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Outlined,
                            icon = Icons.Outlined.LinkOff
                        )
                    }
                }
            }
            state.pairedAdapters.isEmpty() -> {
                AtelierEmptyState(
                    text = stringResource(R.string.diag_bt_empty),
                    actionLabel = stringResource(R.string.diag_bt_refresh),
                    onAction = onRefreshAdapters
                )
            }
            else -> {
                state.pairedAdapters.forEach { adapter ->
                    val matched = state.devices.any { it.matches(adapter.address) }
                    AtelierSelectionCard(
                        title = adapter.name.ifBlank { adapter.address },
                        subtitle = if (matched) {
                            "${adapter.address} · ${stringResource(R.string.diag_bt_matched_workshop)}"
                        } else {
                            adapter.address
                        },
                        selected = state.selectedAdapterAddress == adapter.address,
                        enabled = !state.isConnecting,
                        onClick = { onSelectAdapter(adapter.address) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AtelierButton(
                        text = stringResource(R.string.diag_bt_refresh),
                        onClick = onRefreshAdapters,
                        style = AtelierButtonStyle.Outlined,
                        enabled = !state.isConnecting,
                        icon = Icons.Outlined.Refresh
                    )
                    AtelierButton(
                        text = stringResource(R.string.diag_bt_connect),
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        enabled = state.selectedAdapterAddress != null && !state.isConnecting,
                        loading = state.isConnecting,
                        icon = Icons.Outlined.BluetoothConnected
                    )
                }
            }
        }
    }
}

// endregion

// region Previews

private val previewVehicle = VehicleWithOwner(
    vehicle = Vehicle("v1", "BKL482", "1HGCM82633A004352", "Toyota", "Hilux", 2022, EngineType.DIESEL, "c1", 48200),
    owner = Customer("c1", CustomerType.INDIVIDUAL, "Rosa Quispe Mamani", "40123456", null, "987654321", "ACTIVE")
)

private val previewHubState = DiagnosticsHubUiState(
    selectedTab = DiagnosticsHubTab.SCANNER,
    canUseTelemetry = true,
    canReadTelemetry = true,
    canUseAiDiagnostics = true,
    canReadAiReports = true,
    canReadDevices = true,
    canRegisterDevices = true,
    canManageInstallations = true,
    canReadFaults = true,
    canReadAlerts = true,
    canAcknowledgeAlerts = true,
    canCreateAppointment = true,
    workshopVehicles = listOf(previewVehicle),
    selectedVehicleId = "v1",
    pairedAdapters = listOf(ObdAdapter("OBDLink MX+", "AA:BB:CC:11:22:33")),
    selectedAdapterAddress = "AA:BB:CC:11:22:33",
    linkState = ObdLinkState.CONNECTED,
    connectionInfo = ObdConnectionInfo("ISO 15765-4 CAN (11 bit, 500 kbaud)", "1HGCM82633A004352"),
    devices = listOf(
        ObdDevice("d1", "AA:BB:CC:11:22:33", "BLUETOOTH", DeviceStatus.ACTIVE, "OBDLink MX+", "2.1")
    ),
    tenantAlerts = listOf(
        PredictiveAlert(
            id = "a1",
            vehicleId = "v1",
            alertType = PredictiveAlertType.ENGINE_OVERHEATING_RISK,
            confidenceScore = 0.89,
            message = "Temperatura sostenida por encima de 102 °C en ralenti.",
            status = PredictiveAlertStatus.DISPATCHED,
            recommendedServiceId = "s1",
            createdAt = Instant.parse("2026-10-24T14:00:00Z")
        )
    )
)

@Composable
private fun PreviewHub(state: DiagnosticsHubUiState) {
    DiagnosticsHubContent(
        state = state,
        onBack = {},
        onSelectTab = {},
        onOpenVehiclePicker = {},
        onDismissVehiclePicker = {},
        onVehicleSearchQueryChange = {},
        onSelectVehicle = {},
        onOpenInstallForVehicle = {},
        onDismissInstallForVehicle = {},
        onInstallOdometerChange = {},
        onConfirmInstallOnVehicle = {},
        onUninstallFromVehicle = {},
        onRequestBluetoothPermission = {},
        onRefreshAdapters = {},
        onSelectAdapter = {},
        onConnectAdapter = {},
        onDisconnectAdapter = {},
        onOpenLiveTelemetry = { _, _ -> },
        onOpenVehicleFaults = { _, _ -> },
        onOpenAiHealthReport = { _, _ -> },
        onReportFinding = {},
        onFilterDevices = {},
        onOpenRegisterDevice = {},
        onDismissRegisterDevice = {},
        onIdentifierChange = {},
        onConnectionTypeChange = {},
        onHardwareModelChange = {},
        onFirmwareVersionChange = {},
        onConfirmRegisterDevice = {},
        onOpenStatusSheet = {},
        onDismissStatusSheet = {},
        onUpdateDeviceStatus = {},
        onOpenInstallDevice = {},
        onDismissInstallDevice = {},
        onConfirmInstallDevice = {},
        onOpenUninstallDevice = {},
        onDismissUninstallDevice = {},
        onConfirmUninstallDevice = {},
        onSetAlertsScope = {},
        onRefreshAlerts = {},
        onAcknowledgeAlert = {},
        onOpenConvertAlert = {},
        onDismissConvertAlert = {},
        onConfirmConvertAlert = {},
        onOpenNotifications = {},
        onOpenSyncQueue = {},
        onOpenProfile = {}
    )
}

@Preview(name = "Hub OBD · claro", widthDp = 390, heightDp = 844)
@Composable
private fun HubPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewHub(previewHubState) }

@Preview(name = "Hub OBD · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HubPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewHub(previewHubState) }

@Preview(name = "Hub OBD · alertas oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HubAlertsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PreviewHub(previewHubState.copy(selectedTab = DiagnosticsHubTab.ALERTS))
}

// endregion
