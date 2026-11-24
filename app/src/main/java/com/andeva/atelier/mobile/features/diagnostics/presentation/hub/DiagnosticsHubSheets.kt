package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.presentation.chipKind
import com.andeva.atelier.mobile.features.diagnostics.presentation.labelRes
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

@Composable
internal fun VehiclePickerSheet(
    query: String,
    vehicles: List<VehicleWithOwner>,
    selectedVehicleId: String?,
    onQueryChange: (String) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(title = stringResource(R.string.diag_vehicle_sheet_title), onDismiss = onDismiss) {
        AtelierTextField(
            label = stringResource(R.string.diag_vehicle_search_label),
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.Search,
            placeholder = stringResource(R.string.diag_vehicle_search_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        if (vehicles.isEmpty()) {
            AtelierEmptyState(text = stringResource(R.string.diag_vehicle_empty_search))
        } else {
            vehicles.forEach { item ->
                AtelierListRow(
                    title = "${item.vehicle.brand} ${item.vehicle.model}".trim(),
                    subtitle = item.owner?.displayName,
                    plate = item.vehicle.plate,
                    onClick = { onSelectVehicle(item.vehicle.id) },
                    trailing = if (item.vehicle.id == selectedVehicleId) {
                        {
                            AtelierChip(
                                text = stringResource(R.string.obd_status_active),
                                kind = ChipKind.Ok
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
internal fun InstallOnVehicleSheet(
    state: DiagnosticsHubUiState,
    onOdometerChange: (String) -> Unit,
    onSelectDevice: (ObdDevice) -> Unit,
    onDismiss: () -> Unit
) {
    val activeDevices = state.devices.filter { it.status == DeviceStatus.ACTIVE }
    AtelierBottomSheet(title = stringResource(R.string.obd_install_dialog_title), onDismiss = onDismiss) {
        if (!state.isOnline) AtelierRequiresConnection()
        AtelierTextField(
            label = stringResource(R.string.diag_installation_odometer_label),
            value = state.installOdometerInput,
            onValueChange = onOdometerChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.diag_installation_odometer_hint),
            isError = state.installOdometerInvalid,
            errorText = if (state.installOdometerInvalid) stringResource(R.string.diag_installation_odometer_invalid) else null,
            enabled = !state.isSavingDeviceAction,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
        Text(
            text = stringResource(R.string.diag_installation_choose_device),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        if (activeDevices.isEmpty()) {
            AtelierEmptyState(text = stringResource(R.string.diag_installation_no_active_devices))
        } else {
            activeDevices.forEach { device ->
                val matchesSelectedAdapter = state.selectedAdapterAddress?.let { device.matches(it) } == true
                val subtitle = listOfNotNull(
                    device.hardwareModel,
                    if (matchesSelectedAdapter) stringResource(R.string.diag_bt_matched_workshop) else null
                ).joinToString(" · ")
                AtelierListRow(
                    title = device.deviceIdentifier,
                    subtitle = subtitle.ifBlank { null },
                    onClick = if (state.isOnline && !state.isSavingDeviceAction) ({ onSelectDevice(device) }) else null
                )
            }
        }
    }
}

@Composable
internal fun DeviceActionsSheet(
    device: ObdDevice,
    installation: DeviceInstallationSummary?,
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
        device.status?.let { st ->
            AtelierChip(text = stringResource(st.labelRes()), kind = st.chipKind())
        }
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
    }
}

@Composable
internal fun RegisterDeviceSheet(
    form: RegisterObdDeviceForm,
    isOnline: Boolean,
    isSaving: Boolean,
    error: UiText?,
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
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.obd_register_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.canSubmit && isOnline && !isSaving,
                loading = isSaving
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.obd_field_identifier),
            value = form.deviceIdentifier,
            onValueChange = onIdentifierChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.obd_hint_identifier),
            enabled = !isSaving
        )
        Text(
            text = stringResource(R.string.obd_field_connection_type),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        listOf(
            "BLUETOOTH_BLE" to stringResource(R.string.obd_conn_bluetooth_ble),
            "BLUETOOTH" to stringResource(R.string.obd_conn_bluetooth),
            "WIFI" to stringResource(R.string.obd_conn_wifi),
            "USB" to stringResource(R.string.obd_conn_usb)
        ).forEach { (key, label) ->
            AtelierSelectionCard(
                title = label,
                selected = form.connectionType == key,
                enabled = !isSaving,
                onClick = { onConnectionTypeChange(key) }
            )
        }
        AtelierTextField(
            label = stringResource(R.string.obd_field_model),
            value = form.hardwareModel,
            onValueChange = onHardwareModelChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.obd_hint_model),
            enabled = !isSaving
        )
        AtelierTextField(
            label = stringResource(R.string.obd_field_firmware),
            value = form.firmwareVersion,
            onValueChange = onFirmwareVersionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.obd_hint_firmware),
            enabled = !isSaving
        )
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}
