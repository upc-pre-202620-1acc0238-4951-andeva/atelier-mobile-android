package com.andeva.atelier.mobile.features.diagnostics.presentation.live

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.LatestVehicleTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryHistoryBucket
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySnapshot
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryWarning
import com.andeva.atelier.mobile.features.reception.domain.Vehicle

data class LiveTelemetryUiState(
    val vehicleId: String = "",
    val plate: String? = null,
    val vehicle: Vehicle? = null,
    val canUseTelemetry: Boolean = false,
    val canReadTelemetry: Boolean = false,
    val canReadFaults: Boolean = false,
    val planIncludesTelemetry: Boolean = true,
    val isOnline: Boolean = true,
    val linkState: ObdLinkState = ObdLinkState.DISCONNECTED,
    val liveSample: TelemetrySample? = null,
    val liveWarnings: Set<TelemetryWarning> = emptySet(),
    val remoteTelemetry: LatestVehicleTelemetry? = null,
    val telemetryHistory: List<TelemetryHistoryBucket> = emptyList(),
    val isLoadingRemote: Boolean = false,
    val frozenSnapshot: TelemetrySnapshot? = null,
    val syncState: DiagnosticsSyncState = DiagnosticsSyncState(0, 0),
    val connectedToOtherVehicle: Boolean = false,
    val isReconnecting: Boolean = false,
    val error: UiText? = null
) {
    val isBluetoothSessionActive: Boolean
        get() = linkState == ObdLinkState.CONNECTED ||
            linkState == ObdLinkState.STREAMING ||
            linkState == ObdLinkState.RECONNECTING ||
            linkState == ObdLinkState.LOST

    val isRemoteMode: Boolean
        get() = !isBluetoothSessionActive && canReadTelemetry

    val missingCoolantPid0105: Boolean
        get() = isBluetoothSessionActive && liveSample != null && !liveSample.canBeSent

    val displayPlate: String
        get() = vehicle?.plate ?: plate.orEmpty()
}
