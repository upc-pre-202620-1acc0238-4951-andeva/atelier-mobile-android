package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.presentation.chipKind
import com.andeva.atelier.mobile.features.diagnostics.presentation.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DevicesTabContent(
    state: DiagnosticsHubUiState,
    onFilterDevices: (DeviceStatus?) -> Unit,
    onSelectDevice: (ObdDevice) -> Unit
) {
    val showSkeleton = rememberDelayedLoading(state.isLoadingDevices && state.devices.isEmpty())
    val canManage = state.canManageInstallations || state.canRegisterDevices

    Column(modifier = Modifier.fillMaxSize()) {
        DeviceFilterRow(selected = state.deviceStatusFilter, onFilterChange = onFilterDevices)
        AtelierRefreshBar(visible = state.isLoadingDevices && state.devices.isNotEmpty())
        PullToRefreshBox(
            isRefreshing = state.isLoadingDevices && state.devices.isNotEmpty(),
            onRefresh = { onFilterDevices(state.deviceStatusFilter) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp + LocalFloatingBarInset.current)
            ) {
                when {
                    showSkeleton -> items(4, key = { "bone-$it" }) {
                        AtelierListRow(
                            title = "AA:BB:CC:11:22:33",
                            subtitle = "BLUETOOTH_BLE · ELM327",
                            plate = "BKL482",
                            loading = true
                        )
                    }
                    state.devices.isEmpty() && state.error != null && !state.isOnline -> item(key = "offline") {
                        AtelierOfflineState(
                            modifier = Modifier.padding(horizontal = Spacing.md),
                            onRetry = { onFilterDevices(state.deviceStatusFilter) }
                        )
                    }
                    state.devices.isEmpty() && state.error != null -> item(key = "error") {
                        AtelierErrorState(
                            message = state.error.asString(),
                            modifier = Modifier.padding(horizontal = Spacing.md),
                            onRetry = { onFilterDevices(state.deviceStatusFilter) }
                        )
                    }
                    state.devices.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.obd_empty_list),
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        )
                    }
                    else -> items(state.devices, key = { it.id }) { device ->
                        val install = state.deviceInstallations[device.id]
                        val details = listOfNotNull(
                            device.connectionType,
                            device.hardwareModel,
                            device.firmwareVersion?.let { stringResource(R.string.obd_firmware_format, it) }
                        ).joinToString(" · ")
                        val vehicleLabel = if (install != null) {
                            listOfNotNull(install.vehicleModel.ifBlank { null }, install.ownerName).joinToString(" · ")
                        } else {
                            stringResource(R.string.obd_not_connected)
                        }
                        AtelierListRow(
                            title = device.deviceIdentifier,
                            subtitle = listOf(vehicleLabel, details).filter { it.isNotBlank() }.joinToString(" · "),
                            plate = install?.plate,
                            onClick = if (canManage) ({ onSelectDevice(device) }) else null,
                            trailing = {
                                device.status?.let { st ->
                                    AtelierChip(text = stringResource(st.labelRes()), kind = st.chipKind())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceFilterRow(selected: DeviceStatus?, onFilterChange: (DeviceStatus?) -> Unit) {
    val options = listOf(null, DeviceStatus.ACTIVE, DeviceStatus.INACTIVE)
    val labels = mapOf(
        null to stringResource(R.string.obd_filter_all),
        DeviceStatus.ACTIVE to stringResource(R.string.obd_filter_active),
        DeviceStatus.INACTIVE to stringResource(R.string.obd_filter_inactive)
    )
    AtelierSegmentedControl(
        options = options,
        selected = selected,
        onSelect = onFilterChange,
        label = { labels[it].orEmpty() },
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}
