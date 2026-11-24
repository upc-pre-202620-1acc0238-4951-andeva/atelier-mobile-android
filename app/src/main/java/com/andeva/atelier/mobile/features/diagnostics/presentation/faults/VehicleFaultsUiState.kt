package com.andeva.atelier.mobile.features.diagnostics.presentation.faults

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.diagnostics.domain.ClearFaultsResult
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.FreezeFrame
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.reception.domain.Vehicle

data class VehicleFaultsUiState(
    val vehicleId: String = "",
    val plate: String? = null,
    val vehicle: Vehicle? = null,
    val faults: List<VehicleFault> = emptyList(),
    val freezeFrame: FreezeFrame? = null,
    val lastClearResult: ClearFaultsResult? = null,
    val syncState: DiagnosticsSyncState = DiagnosticsSyncState(0, 0),
    val linkState: ObdLinkState = ObdLinkState.DISCONNECTED,
    val canReadFaults: Boolean = false,
    val canScanFaults: Boolean = false,
    val canResolveFaults: Boolean = false,
    val planIncludesTelemetry: Boolean = true,
    val isOnline: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoaded: Boolean = false,
    val isScanning: Boolean = false,
    val isClearing: Boolean = false,
    val confirmClearOpen: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val displayPlate: String get() = vehicle?.plate ?: plate.orEmpty()

    val isBluetoothConnected: Boolean
        get() = linkState == ObdLinkState.CONNECTED ||
            linkState == ObdLinkState.STREAMING ||
            linkState == ObdLinkState.RECONNECTING
}
