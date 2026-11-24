package com.andeva.atelier.mobile.features.diagnostics.presentation.faults

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.application.ClearFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveDiagnosticsSyncStateUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveVehicleFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.RefreshVehicleFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ScanFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.presentation.toDiagnosticsUiText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveVehicleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleFaultsViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeVehicle: ObserveVehicleUseCase,
    observeVehicleFaults: ObserveVehicleFaultsUseCase,
    observeSyncState: ObserveDiagnosticsSyncStateUseCase,
    observeLiveTelemetry: ObserveLiveTelemetryUseCase,
    connectivity: ConnectivityObserver,
    private val refreshVehicleFaults: RefreshVehicleFaultsUseCase,
    private val scanFaultsUseCase: ScanFaultsUseCase,
    private val clearFaultsUseCase: ClearFaultsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: String = savedStateHandle["vehicleId"] ?: ""
    private val initialPlate: String? = savedStateHandle["plate"]

    private val _uiState = MutableStateFlow(VehicleFaultsUiState(vehicleId = vehicleId, plate = initialPlate))
    val uiState: StateFlow<VehicleFaultsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canUseTelemetry = session?.canUseTelemetry() == true
                val planIncludes = session?.plan?.iotTelemetryEnabled ?: true
                val canResolve = session?.has(Permission.FAULTS_RESOLVE) == true && planIncludes
                _uiState.update {
                    it.copy(
                        planIncludesTelemetry = planIncludes,
                        canReadFaults = session?.has(Permission.FAULTS_READ) == true,
                        canScanFaults = canUseTelemetry || canResolve,
                        canResolveFaults = canResolve
                    )
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
                observeVehicleFaults(vehicleId).collect { faults ->
                    _uiState.update { it.copy(faults = faults, isLoaded = true) }
                }
            }
            viewModelScope.launch {
                observeSyncState(vehicleId).collect { sync ->
                    _uiState.update { it.copy(syncState = sync) }
                }
            }
        } else {
            _uiState.update { it.copy(syncState = DiagnosticsSyncState(0, 0), isLoaded = true) }
        }

        viewModelScope.launch {
            observeLiveTelemetry().collect { live ->
                _uiState.update { it.copy(linkState = live.state) }
            }
        }

        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        refresh()
    }

    fun refresh() {
        if (vehicleId.isBlank() || _uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            refreshVehicleFaults(vehicleId)
                .onSuccess {
                    _uiState.update { it.copy(isRefreshing = false, isLoaded = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRefreshing = false, isLoaded = true, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun scanDtcs() {
        val state = _uiState.value
        if (!state.canScanFaults || !state.isBluetoothConnected || state.isScanning || vehicleId.isBlank()) return
        _uiState.update { it.copy(isScanning = true, error = null, notice = null, lastClearResult = null) }
        viewModelScope.launch {
            scanFaultsUseCase(vehicleId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            freezeFrame = result.freezeFrame,
                            notice = if (result.isClean) UiText.Resource(R.string.diag_faults_scan_clean) else null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isScanning = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun openClearConfirm() {
        val state = _uiState.value
        if (!state.canResolveFaults || !state.isBluetoothConnected || state.isClearing) return
        _uiState.update { it.copy(confirmClearOpen = true, error = null) }
    }

    fun dismissClearConfirm() = _uiState.update {
        if (it.isClearing) it else it.copy(confirmClearOpen = false)
    }

    fun confirmClearDtcs() {
        val state = _uiState.value
        if (!state.canResolveFaults || !state.isBluetoothConnected || state.isClearing || vehicleId.isBlank()) return
        _uiState.update { it.copy(isClearing = true, error = null, notice = null) }
        viewModelScope.launch {
            clearFaultsUseCase(vehicleId)
                .onSuccess { result ->
                    val noticeText = if (result.isClean) {
                        UiText.Resource(R.string.diag_faults_clear_result_clean, result.cleared.size)
                    } else {
                        UiText.Resource(
                            R.string.diag_faults_clear_result_persistent,
                            result.cleared.size,
                            result.persistent.size
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isClearing = false,
                            confirmClearOpen = false,
                            lastClearResult = result,
                            freezeFrame = if (result.isClean) null else it.freezeFrame,
                            notice = noticeText
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isClearing = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }
}
