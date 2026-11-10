package com.andeva.atelier.mobile.features.main.presentation.obd

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

data class DeviceInstallationInfo(
    val installationId: String,
    val deviceId: String,
    val vehicleId: String,
    val plate: String,
    val vehicleModel: String,
    val ownerName: String?
)

data class RegisterObdForm(
    val deviceIdentifier: String = "",
    val connectionType: String = "BLUETOOTH",
    val hardwareModel: String = "",
    val firmwareVersion: String = ""
) {
    val canSubmit: Boolean get() = deviceIdentifier.isNotBlank() && connectionType.isNotBlank()
}

data class ObdUiState(
    val devices: List<ObdDevice> = emptyList(),
    val statusFilter: DeviceStatus? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val registerForm: RegisterObdForm? = null,
    val editingDevice: ObdDevice? = null,
    val installingDevice: ObdDevice? = null,
    val uninstallingDevice: ObdDevice? = null,
    val deviceInstallations: Map<String, DeviceInstallationInfo> = emptyMap(),
    val workshopVehicles: List<VehicleWithOwner> = emptyList(),
    val canReadDevices: Boolean = false,
    val canRegisterDevices: Boolean = false,
    val canManageInstallations: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val activeCount: Int get() = devices.count { it.status == DeviceStatus.ACTIVE }
    val issueCount: Int get() = devices.count { it.status == DeviceStatus.BROKEN || it.status == DeviceStatus.LOST }
}
