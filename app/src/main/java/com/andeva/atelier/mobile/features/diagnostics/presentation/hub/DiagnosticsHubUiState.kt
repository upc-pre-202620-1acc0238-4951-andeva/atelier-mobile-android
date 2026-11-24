package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapter
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

enum class DiagnosticsHubTab { SCANNER, DEVICES, ALERTS }

enum class AlertsScope { TENANT, VEHICLE }

data class DeviceInstallationSummary(
    val installationId: String,
    val deviceId: String,
    val vehicleId: String,
    val plate: String,
    val vehicleModel: String,
    val ownerName: String?
)

data class RegisterObdDeviceForm(
    val deviceIdentifier: String = "",
    val connectionType: String = "BLUETOOTH_BLE",
    val hardwareModel: String = "",
    val firmwareVersion: String = ""
) {
    val canSubmit: Boolean get() = deviceIdentifier.isNotBlank() && connectionType.isNotBlank()
}

data class DiagnosticsHubUiState(
    val selectedTab: DiagnosticsHubTab = DiagnosticsHubTab.SCANNER,
    // Plan & permisos
    val planIncludesTelemetry: Boolean = true,
    val planIncludesAi: Boolean = true,
    val canUseTelemetry: Boolean = false,
    val canReadTelemetry: Boolean = false,
    val canUseAiDiagnostics: Boolean = false,
    val canReadAiReports: Boolean = false,
    val canReadDevices: Boolean = false,
    val canRegisterDevices: Boolean = false,
    val canManageInstallations: Boolean = false,
    val canReadFaults: Boolean = false,
    val canResolveFaults: Boolean = false,
    val canReadAlerts: Boolean = false,
    val canAcknowledgeAlerts: Boolean = false,
    val canCreateAppointment: Boolean = false,
    val canReportFinding: Boolean = false,
    // Estado global
    val isOnline: Boolean = true,
    val unreadNotificationsCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val error: UiText? = null,
    val notice: UiText? = null,
    // Vehículos
    val workshopVehicles: List<VehicleWithOwner> = emptyList(),
    val vehiclesLoading: Boolean = false,
    val vehicleWorkOrderIds: Map<String, String> = emptyMap(),
    val selectedVehicleId: String? = null,
    val vehiclePickerOpen: Boolean = false,
    val vehicleSearchQuery: String = "",
    // Escáner en vivo & vinculación (Tab 1)
    val activeInstallation: DeviceInstallation? = null,
    val isLoadingInstallation: Boolean = false,
    val installForVehicleSheetOpen: Boolean = false,
    val installOdometerInput: String = "",
    val installOdometerInvalid: Boolean = false,
    val pairedAdapters: List<ObdAdapter> = emptyList(),
    val bluetoothPermissionRequired: Boolean = false,
    val bluetoothDisabled: Boolean = false,
    val selectedAdapterAddress: String? = null,
    val linkState: ObdLinkState = ObdLinkState.DISCONNECTED,
    val connectionInfo: ObdConnectionInfo? = null,
    val isConnecting: Boolean = false,
    val vehicleSyncState: DiagnosticsSyncState = DiagnosticsSyncState(0, 0),
    // Dispositivos (Tab 2)
    val devices: List<ObdDevice> = emptyList(),
    val deviceStatusFilter: DeviceStatus? = null,
    val isLoadingDevices: Boolean = false,
    val deviceInstallations: Map<String, DeviceInstallationSummary> = emptyMap(),
    val registerForm: RegisterObdDeviceForm? = null,
    val editingStatusDevice: ObdDevice? = null,
    val installingDevice: ObdDevice? = null,
    val uninstallingDevice: ObdDevice? = null,
    val isSavingDeviceAction: Boolean = false,
    // Alertas predictivas (Tab 3)
    val alertsScope: AlertsScope = AlertsScope.TENANT,
    val tenantAlerts: List<PredictiveAlert> = emptyList(),
    val vehicleAlerts: List<PredictiveAlert> = emptyList(),
    val isLoadingAlerts: Boolean = false,
    val convertingAlert: PredictiveAlert? = null,
    val convertAlertDateText: String = "",
    val convertAlertTimeText: String = "10:00",
    val convertAlertDurationText: String = "60",
    val convertAlertReason: String = "",
    val isActingOnAlert: Boolean = false
) {
    val availableTabs: List<DiagnosticsHubTab>
        get() = buildList {
            val hasScannerTelemetry = planIncludesTelemetry &&
                (canUseTelemetry || canReadTelemetry || canReadFaults || canResolveFaults)
            val hasScannerAi = planIncludesAi && (canUseAiDiagnostics || canReadAiReports)
            if (hasScannerTelemetry || hasScannerAi) {
                add(DiagnosticsHubTab.SCANNER)
            }
            if (planIncludesTelemetry && (canReadDevices || canRegisterDevices)) add(DiagnosticsHubTab.DEVICES)
            if (planIncludesTelemetry && canReadAlerts) add(DiagnosticsHubTab.ALERTS)
        }

    val canConnectBluetooth: Boolean
        get() = planIncludesTelemetry && (canUseTelemetry || canResolveFaults)

    val selectedVehicle: VehicleWithOwner?
        get() = workshopVehicles.firstOrNull { it.vehicle.id == selectedVehicleId }

    val selectedVehicleWorkOrderId: String?
        get() = selectedVehicleId?.let { vehicleWorkOrderIds[it] }

    val filteredVehicles: List<VehicleWithOwner>
        get() {
            val q = vehicleSearchQuery.trim().lowercase()
            if (q.isEmpty()) return workshopVehicles
            return workshopVehicles.filter { item ->
                item.vehicle.plate.lowercase().contains(q) ||
                    item.vehicle.brand.lowercase().contains(q) ||
                    item.vehicle.model.lowercase().contains(q) ||
                    (item.owner?.displayName?.lowercase()?.contains(q) == true)
            }
        }

    val visibleAlerts: List<PredictiveAlert>
        get() = when (alertsScope) {
            AlertsScope.TENANT -> tenantAlerts
            AlertsScope.VEHICLE -> vehicleAlerts
        }

    val isBluetoothConnected: Boolean
        get() = linkState == ObdLinkState.CONNECTED ||
            linkState == ObdLinkState.STREAMING ||
            linkState == ObdLinkState.RECONNECTING
}
