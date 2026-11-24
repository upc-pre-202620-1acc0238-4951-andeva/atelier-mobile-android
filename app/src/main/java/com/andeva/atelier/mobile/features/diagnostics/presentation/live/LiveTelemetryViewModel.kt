package com.andeva.atelier.mobile.features.diagnostics.presentation.live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.application.FreezeTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetLatestVehicleTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetVehicleTelemetryHistoryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveDiagnosticsSyncStateUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ReconnectObdUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.StartLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.StopLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.presentation.toDiagnosticsUiText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveVehicleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class LiveTelemetryViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeVehicle: ObserveVehicleUseCase,
    observeLiveTelemetry: ObserveLiveTelemetryUseCase,
    observeSyncState: ObserveDiagnosticsSyncStateUseCase,
    connectivity: ConnectivityObserver,
    private val startLiveTelemetry: StartLiveTelemetryUseCase,
    private val stopLiveTelemetry: StopLiveTelemetryUseCase,
    private val reconnectObd: ReconnectObdUseCase,
    private val freezeTelemetry: FreezeTelemetryUseCase,
    private val getLatestTelemetry: GetLatestVehicleTelemetryUseCase,
    private val getTelemetryHistory: GetVehicleTelemetryHistoryUseCase,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: String = savedStateHandle["vehicleId"] ?: ""
    private val initialPlate: String? = savedStateHandle["plate"]

    private val _uiState = MutableStateFlow(LiveTelemetryUiState(vehicleId = vehicleId, plate = initialPlate))
    val uiState: StateFlow<LiveTelemetryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canUse = session?.canUseTelemetry() == true
                val canRead = session?.canReadTelemetry() == true
                val planIncludes = session?.plan?.iotTelemetryEnabled ?: true
                _uiState.update {
                    it.copy(
                        canUseTelemetry = canUse,
                        canReadTelemetry = canRead,
                        canReadFaults = session?.has(Permission.FAULTS_READ) == true,
                        planIncludesTelemetry = planIncludes
                    )
                }
                if (canRead && !_uiState.value.isBluetoothSessionActive) {
                    loadRemoteTelemetry()
                }
            }
        }

        if (vehicleId.isNotBlank()) {
            viewModelScope.launch {
                observeVehicle(vehicleId).collect { vehicle ->
                    _uiState.update { it.copy(vehicle = vehicle) }
                }
            }
            viewModelScope.launch {
                observeSyncState(vehicleId).collect { sync ->
                    _uiState.update { it.copy(syncState = sync) }
                }
            }
        }

        viewModelScope.launch {
            observeLiveTelemetry().collect { live ->
                val previousState = _uiState.value.linkState
                val matchesVehicle = live.vehicleId == null || live.vehicleId == vehicleId
                val activeOnOther = !matchesVehicle && (
                    live.state == ObdLinkState.CONNECTED ||
                        live.state == ObdLinkState.STREAMING ||
                        live.state == ObdLinkState.RECONNECTING
                    )
                _uiState.update {
                    if (activeOnOther) {
                        it.copy(
                            linkState = ObdLinkState.DISCONNECTED,
                            liveSample = null,
                            liveWarnings = emptySet(),
                            connectedToOtherVehicle = true
                        )
                    } else {
                        it.copy(
                            linkState = live.state,
                            liveSample = live.sample,
                            liveWarnings = live.warnings,
                            connectedToOtherVehicle = false
                        )
                    }
                }
                if (!activeOnOther && live.state == ObdLinkState.CONNECTED && previousState == ObdLinkState.DISCONNECTED) {
                    startStreamingIfEligible()
                }
            }
        }

        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun reconnect() {
        val state = _uiState.value
        if (!state.canUseTelemetry || state.isReconnecting) return
        _uiState.update { it.copy(isReconnecting = true, error = null) }
        viewModelScope.launch {
            reconnectObd()
                .onSuccess {
                    _uiState.update { it.copy(isReconnecting = false) }
                    if (vehicleId.isNotBlank()) {
                        startStreamingIfEligible()
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isReconnecting = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun startStreamingIfEligible() {
        val state = _uiState.value
        if (!state.canUseTelemetry || vehicleId.isBlank()) return
        if (state.linkState == ObdLinkState.STREAMING || state.linkState == ObdLinkState.RECONNECTING) return
        viewModelScope.launch {
            startLiveTelemetry(vehicleId).onFailure { e ->
                _uiState.update { it.copy(error = e.toDiagnosticsUiText()) }
            }
        }
    }

    fun stopStreaming() {
        viewModelScope.launch {
            stopLiveTelemetry()
        }
    }

    fun freezeSample() {
        freezeTelemetry()
            .onSuccess { snapshot ->
                _uiState.update { it.copy(frozenSnapshot = snapshot, error = null) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.toDiagnosticsUiText()) }
            }
    }

    fun dismissFrozenSnapshot() = _uiState.update { it.copy(frozenSnapshot = null) }

    fun loadRemoteTelemetry() {
        val state = _uiState.value
        if (!state.canReadTelemetry || vehicleId.isBlank() || state.isLoadingRemote) return
        _uiState.update { it.copy(isLoadingRemote = true, error = null) }
        viewModelScope.launch {
            val latestResult = getLatestTelemetry(vehicleId)
            val now = clock.instant()
            val from = now.minus(24, ChronoUnit.HOURS)
            val historyResult = getTelemetryHistory(vehicleId, from = from, to = now, bucket = "1 hour")
            _uiState.update { current ->
                current.copy(
                    isLoadingRemote = false,
                    remoteTelemetry = latestResult.getOrNull() ?: current.remoteTelemetry,
                    telemetryHistory = historyResult.getOrNull() ?: current.telemetryHistory,
                    error = latestResult.exceptionOrNull()?.toDiagnosticsUiText()
                )
            }
        }
    }
}
