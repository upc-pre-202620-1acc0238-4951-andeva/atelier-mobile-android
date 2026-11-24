package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.diagnostics.application.AcknowledgePredictiveAlertUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ConnectObdUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ConvertAlertToAppointmentUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.DisconnectObdUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetActiveInstallationUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetObdDevicesUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetPairedObdAdaptersUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetTenantPredictiveAlertsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.InstallObdDeviceUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveDiagnosticsSyncStateUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObservePredictiveAlertsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.RefreshPredictiveAlertsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.RegisterObdDeviceUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.UninstallObdDeviceUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.UpdateObdDeviceStatusUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothDisabledException
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothPermissionDeniedException
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsValidation
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.toDiagnosticsUiText
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.EnrichReceptionReferencesUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveWorkshopVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import com.andeva.atelier.mobile.features.tasks.application.RefreshMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskDao
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiagnosticsHubViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observePendingCount: ObservePendingOperationCountUseCase,
    connectivity: ConnectivityObserver,
    pushInbox: PushInbox,
    private val observeWorkshopVehicles: ObserveWorkshopVehiclesUseCase,
    private val enrichReferences: EnrichReceptionReferencesUseCase,
    private val refreshMyTasks: RefreshMyTasksUseCase,
    private val refreshBranchWorkOrders: RefreshBranchWorkOrdersUseCase,
    private val taskDao: TaskDao,
    private val workOrderDao: WorkOrderDao,
    private val getActiveInstallation: GetActiveInstallationUseCase,
    private val installObdDevice: InstallObdDeviceUseCase,
    private val uninstallObdDevice: UninstallObdDeviceUseCase,
    private val getObdDevices: GetObdDevicesUseCase,
    private val registerObdDevice: RegisterObdDeviceUseCase,
    private val updateObdDeviceStatus: UpdateObdDeviceStatusUseCase,
    private val getPairedObdAdapters: GetPairedObdAdaptersUseCase,
    private val connectObd: ConnectObdUseCase,
    private val disconnectObd: DisconnectObdUseCase,
    private val observeLiveTelemetry: ObserveLiveTelemetryUseCase,
    private val observeDiagnosticsSyncState: ObserveDiagnosticsSyncStateUseCase,
    private val getTenantPredictiveAlerts: GetTenantPredictiveAlertsUseCase,
    private val refreshPredictiveAlerts: RefreshPredictiveAlertsUseCase,
    private val observePredictiveAlerts: ObservePredictiveAlertsUseCase,
    private val acknowledgePredictiveAlert: AcknowledgePredictiveAlertUseCase,
    private val convertAlertToAppointment: ConvertAlertToAppointmentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialVehicleId: String? = savedStateHandle["initialVehicleId"]
    private val selectedVehicleIdFlow = MutableStateFlow(initialVehicleId)

    private val _uiState = MutableStateFlow(DiagnosticsHubUiState(selectedVehicleId = initialVehicleId))
    val uiState: StateFlow<DiagnosticsHubUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val planTelemetry = session?.plan?.iotTelemetryEnabled ?: true
                val planAi = session?.plan?.aiDiagnosticsEnabled ?: true
                _uiState.update { current ->
                    val next = current.copy(
                        planIncludesTelemetry = planTelemetry,
                        planIncludesAi = planAi,
                        canUseTelemetry = session?.canUseTelemetry() == true,
                        canReadTelemetry = session?.canReadTelemetry() == true,
                        canUseAiDiagnostics = session?.canUseAiDiagnostics() == true,
                        canReadAiReports = session?.canReadAiReports() == true,
                        canReadDevices = session?.has(Permission.DEVICES_READ) == true,
                        canRegisterDevices = session?.has(Permission.DEVICES_REGISTER) == true,
                        canManageInstallations = session?.has(Permission.INSTALLATIONS_MANAGE) == true,
                        canReadFaults = session?.has(Permission.FAULTS_READ) == true,
                        canResolveFaults = session?.has(Permission.FAULTS_RESOLVE) == true && planTelemetry,
                        canReadAlerts = session?.has(Permission.PREDICTIVE_ALERTS_READ) == true,
                        canAcknowledgeAlerts = session?.has(Permission.PREDICTIVE_ALERTS_ACKNOWLEDGE) == true,
                        canCreateAppointment = session?.has(Permission.APPOINTMENTS_CREATE) == true,
                        canReportFinding = session?.has(Permission.PROPOSALS_CREATE) == true
                    )
                    val tabs = next.availableTabs
                    if (tabs.isNotEmpty() && next.selectedTab !in tabs) {
                        next.copy(selectedTab = tabs.first())
                    } else {
                        next
                    }
                }
                if (session != null && (planTelemetry || planAi)) {
                    if (session.has(Permission.TASKS_READ) || session.has(Permission.WORK_ORDERS_READ)) {
                        _uiState.update { it.copy(vehiclesLoading = it.workshopVehicles.isEmpty()) }
                    }
                    if (session.has(Permission.TASKS_READ)) launch {
                        refreshMyTasks()
                        _uiState.update { it.copy(vehiclesLoading = false) }
                    }
                    if (session.has(Permission.WORK_ORDERS_READ)) launch {
                        refreshBranchWorkOrders()
                        _uiState.update { it.copy(vehiclesLoading = false) }
                    }
                }
                if (session != null && planTelemetry) {
                    if (session.has(Permission.DEVICES_READ)) loadDevices(_uiState.value.deviceStatusFilter)
                    if (session.has(Permission.PREDICTIVE_ALERTS_READ)) loadTenantAlerts()
                }
            }
        }

        viewModelScope.launch {
            observeSession().flatMapLatest { session ->
                val branchId = session?.branchId.orEmpty()
                val workOrdersFlow = if (branchId.isNotBlank()) workOrderDao.observeBranch(branchId) else flowOf(emptyList())
                combine(observeWorkshopVehicles(), taskDao.observeAll(), workOrdersFlow) { vehicles, tasks, workOrders ->
                    val woByVehicle = buildMap {
                        workOrders.forEach { put(it.workOrder.vehicleId, it.workOrder.id) }
                        tasks.forEach { putIfAbsent(it.task.vehicleId, it.task.workOrderId) }
                    }
                    val priorityIds = (tasks.map { it.task.vehicleId } + workOrders.map { it.workOrder.vehicleId })
                        .filter { it.isNotBlank() }
                        .distinct()
                    val cachedIds = vehicles.map { it.vehicle.id }.toSet()
                    val missing = (priorityIds + listOfNotNull(selectedVehicleIdFlow.value))
                        .distinct()
                        .filter { it.isNotBlank() && it !in cachedIds }
                    if (missing.isNotEmpty()) {
                        launch { enrichReferences(missing, emptyList()) }
                    }
                    val prioritySet = priorityIds.toSet()
                    val ordered = vehicles.sortedByDescending { it.vehicle.id in prioritySet }
                    ordered to woByVehicle
                }
            }.collect { (vehicles, woByVehicle) ->
                _uiState.update { state ->
                    val chosen = state.selectedVehicleId?.takeIf { id -> vehicles.any { it.vehicle.id == id } }
                        ?: initialVehicleId
                        ?: vehicles.firstOrNull()?.vehicle?.id
                    if (chosen != selectedVehicleIdFlow.value) {
                        selectedVehicleIdFlow.value = chosen
                    }
                    state.copy(
                        workshopVehicles = vehicles,
                        vehicleWorkOrderIds = woByVehicle,
                        selectedVehicleId = chosen
                    )
                }
                if (_uiState.value.planIncludesTelemetry) {
                    loadDeviceInstallations(vehicles)
                    selectedVehicleIdFlow.value?.let { loadActiveInstallation(it) }
                }
            }
        }

        viewModelScope.launch {
            selectedVehicleIdFlow.flatMapLatest { vehicleId ->
                if (vehicleId.isNullOrBlank()) flowOf(DiagnosticsSyncState(0, 0))
                else observeDiagnosticsSyncState(vehicleId)
            }.collect { sync ->
                _uiState.update { it.copy(vehicleSyncState = sync) }
            }
        }

        viewModelScope.launch {
            selectedVehicleIdFlow.flatMapLatest { vehicleId ->
                if (vehicleId.isNullOrBlank()) flowOf(emptyList())
                else observePredictiveAlerts(vehicleId)
            }.collect { alerts ->
                _uiState.update { it.copy(vehicleAlerts = alerts) }
            }
        }

        viewModelScope.launch {
            observeLiveTelemetry().collect { live ->
                _uiState.update { state ->
                    val chosen = state.selectedVehicleId ?: live.vehicleId
                    state.copy(
                        linkState = live.state,
                        selectedVehicleId = chosen
                    )
                }
            }
        }

        viewModelScope.launch {
            observePendingCount().collect { count -> _uiState.update { it.copy(pendingSyncCount = count) } }
        }

        viewModelScope.launch {
            connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } }
        }

        viewModelScope.launch {
            pushInbox.history.collect { records ->
                _uiState.update { it.copy(unreadNotificationsCount = records.count { r -> !r.read }) }
            }
        }

        loadPairedAdapters()
    }

    fun selectTab(tab: DiagnosticsHubTab) {
        if (tab !in _uiState.value.availableTabs) return
        _uiState.update { it.copy(selectedTab = tab, error = null) }
        when (tab) {
            DiagnosticsHubTab.SCANNER -> {
                loadPairedAdapters()
                _uiState.value.selectedVehicleId?.let { loadActiveInstallation(it) }
            }
            DiagnosticsHubTab.DEVICES -> loadDevices(_uiState.value.deviceStatusFilter)
            DiagnosticsHubTab.ALERTS -> refreshAlerts()
        }
    }

    fun clearNotices() = _uiState.update { it.copy(error = null, notice = null) }

    // region Vehículo e instalación en Escáner

    fun openVehiclePicker() = _uiState.update { it.copy(vehiclePickerOpen = true, vehicleSearchQuery = "") }

    fun dismissVehiclePicker() = _uiState.update { it.copy(vehiclePickerOpen = false) }

    fun onVehicleSearchQueryChange(query: String) = _uiState.update { it.copy(vehicleSearchQuery = query) }

    fun selectVehicle(vehicleId: String) {
        selectedVehicleIdFlow.value = vehicleId
        _uiState.update { it.copy(selectedVehicleId = vehicleId, vehiclePickerOpen = false, error = null) }
        loadActiveInstallation(vehicleId)
        if (_uiState.value.canReadAlerts) {
            viewModelScope.launch { refreshPredictiveAlerts(vehicleId) }
        }
    }

    private fun loadActiveInstallation(vehicleId: String) {
        if (!_uiState.value.canReadDevices || !_uiState.value.planIncludesTelemetry || !_uiState.value.isOnline) return
        _uiState.update { it.copy(isLoadingInstallation = true) }
        viewModelScope.launch {
            getActiveInstallation(vehicleId)
                .onSuccess { installation ->
                    _uiState.update { it.copy(isLoadingInstallation = false, activeInstallation = installation) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingInstallation = false) }
                }
        }
    }

    fun openInstallForSelectedVehicle() {
        val state = _uiState.value
        if (!state.canManageInstallations || !state.isOnline || state.selectedVehicleId == null) return
        if (state.devices.isEmpty()) loadDevices(DeviceStatus.ACTIVE)
        val odometer = state.selectedVehicle?.vehicle?.currentMileage?.toString().orEmpty()
        _uiState.update {
            it.copy(
                installForVehicleSheetOpen = true,
                installOdometerInput = odometer,
                installOdometerInvalid = false,
                error = null
            )
        }
    }

    fun dismissInstallForSelectedVehicle() = _uiState.update {
        if (it.isSavingDeviceAction) it else it.copy(installForVehicleSheetOpen = false)
    }

    fun onInstallOdometerChange(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { it.copy(installOdometerInput = digits, installOdometerInvalid = false) }
    }

    fun confirmInstallOnSelectedVehicle(device: ObdDevice) {
        val state = _uiState.value
        val vehicleId = state.selectedVehicleId ?: return
        if (state.isSavingDeviceAction || !state.isOnline) return
        val odometerText = state.installOdometerInput.trim()
        val odometerKm = if (odometerText.isEmpty()) null else odometerText.toIntOrNull()
        if ((odometerText.isNotEmpty() && odometerKm == null) || !DiagnosticsValidation.isOdometer(odometerKm)) {
            _uiState.update { it.copy(installOdometerInvalid = true) }
            return
        }
        _uiState.update { it.copy(isSavingDeviceAction = true, error = null) }
        viewModelScope.launch {
            installObdDevice(device.id, vehicleId, odometerKm)
                .onSuccess { installation ->
                    val vehicle = _uiState.value.selectedVehicle
                    val summary = vehicle?.let {
                        DeviceInstallationSummary(
                            installationId = installation.id,
                            deviceId = device.id,
                            vehicleId = vehicleId,
                            plate = it.vehicle.plate,
                            vehicleModel = "${it.vehicle.brand} ${it.vehicle.model}".trim(),
                            ownerName = it.owner?.displayName
                        )
                    }
                    _uiState.update { current ->
                        current.copy(
                            isSavingDeviceAction = false,
                            installForVehicleSheetOpen = false,
                            activeInstallation = installation,
                            deviceInstallations = if (summary != null) {
                                current.deviceInstallations + (device.id to summary)
                            } else current.deviceInstallations,
                            notice = UiText.Resource(R.string.obd_install_success, vehicle?.vehicle?.plate.orEmpty())
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSavingDeviceAction = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun uninstallFromSelectedVehicle() {
        val state = _uiState.value
        val installation = state.activeInstallation ?: return
        if (!state.canManageInstallations || !state.isOnline || state.isSavingDeviceAction) return
        _uiState.update { it.copy(isSavingDeviceAction = true, error = null) }
        viewModelScope.launch {
            uninstallObdDevice(installation.id, state.selectedVehicle?.vehicle?.currentMileage)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isSavingDeviceAction = false,
                            activeInstallation = null,
                            deviceInstallations = current.deviceInstallations - installation.deviceId,
                            notice = UiText.Resource(R.string.obd_uninstall_success)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSavingDeviceAction = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    // endregion

    // region Bluetooth ELM327

    fun loadPairedAdapters() {
        getPairedObdAdapters()
            .onSuccess { adapters ->
                _uiState.update { state ->
                    val chosen = state.selectedAdapterAddress?.takeIf { addr -> adapters.any { it.address == addr } }
                        ?: adapters.firstOrNull { adapter -> state.devices.any { it.matches(adapter.address) } }?.address
                        ?: adapters.firstOrNull()?.address
                    state.copy(
                        pairedAdapters = adapters,
                        selectedAdapterAddress = chosen,
                        bluetoothPermissionRequired = false,
                        bluetoothDisabled = false
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        pairedAdapters = emptyList(),
                        bluetoothPermissionRequired = e is BluetoothPermissionDeniedException,
                        bluetoothDisabled = e is BluetoothDisabledException
                    )
                }
            }
    }

    fun onBluetoothPermissionResult(granted: Boolean) {
        if (granted) {
            loadPairedAdapters()
        } else {
            _uiState.update { it.copy(bluetoothPermissionRequired = true) }
        }
    }

    fun selectAdapter(address: String) = _uiState.update { it.copy(selectedAdapterAddress = address, error = null) }

    fun connectSelectedAdapter() {
        val state = _uiState.value
        val address = state.selectedAdapterAddress ?: return
        if (!state.canConnectBluetooth || state.isConnecting) return
        _uiState.update { it.copy(isConnecting = true, error = null, notice = null) }
        viewModelScope.launch {
            connectObd(address)
                .onSuccess { info ->
                    _uiState.update { it.copy(isConnecting = false, connectionInfo = info) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isConnecting = false, connectionInfo = null, error = e.toDiagnosticsUiText())
                    }
                }
        }
    }

    fun disconnectAdapter() {
        viewModelScope.launch {
            disconnectObd()
            _uiState.update { it.copy(connectionInfo = null) }
        }
    }

    // endregion

    // region Dispositivos OBD-II (Tab 2)

    fun loadDevices(filter: DeviceStatus? = _uiState.value.deviceStatusFilter) {
        if (!_uiState.value.canReadDevices || !_uiState.value.planIncludesTelemetry) return
        _uiState.update { it.copy(isLoadingDevices = true, deviceStatusFilter = filter, error = null) }
        viewModelScope.launch {
            getObdDevices(status = filter)
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingDevices = false, devices = list) }
                    loadDeviceInstallations(_uiState.value.workshopVehicles)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingDevices = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    private fun loadDeviceInstallations(vehicles: List<VehicleWithOwner>) {
        if (vehicles.isEmpty() || !_uiState.value.canReadDevices || !_uiState.value.isOnline) return
        viewModelScope.launch {
            val map = coroutineScope {
                vehicles.map { v ->
                    async {
                        val active = getActiveInstallation(v.vehicle.id).getOrNull()
                        if (active != null && active.isActive) {
                            active.deviceId to DeviceInstallationSummary(
                                installationId = active.id,
                                deviceId = active.deviceId,
                                vehicleId = v.vehicle.id,
                                plate = v.vehicle.plate,
                                vehicleModel = "${v.vehicle.brand} ${v.vehicle.model}".trim(),
                                ownerName = v.owner?.displayName
                            )
                        } else null
                    }
                }.awaitAll().filterNotNull().toMap()
            }
            _uiState.update { it.copy(deviceInstallations = map) }
        }
    }

    fun onOpenRegisterDevice() {
        if (!_uiState.value.canRegisterDevices || !_uiState.value.isOnline) return
        _uiState.update { it.copy(registerForm = RegisterObdDeviceForm(), error = null, notice = null) }
    }

    fun onDismissRegisterDevice() = _uiState.update {
        if (it.isSavingDeviceAction) it else it.copy(registerForm = null, error = null)
    }

    fun onIdentifierChange(value: String) = editRegisterForm { it.copy(deviceIdentifier = value) }
    fun onConnectionTypeChange(value: String) = editRegisterForm { it.copy(connectionType = value) }
    fun onHardwareModelChange(value: String) = editRegisterForm { it.copy(hardwareModel = value) }
    fun onFirmwareVersionChange(value: String) = editRegisterForm { it.copy(firmwareVersion = value) }

    private fun editRegisterForm(transform: (RegisterObdDeviceForm) -> RegisterObdDeviceForm) = _uiState.update {
        val form = it.registerForm ?: return@update it
        if (it.isSavingDeviceAction) it else it.copy(registerForm = transform(form), error = null)
    }

    fun onConfirmRegisterDevice() {
        val state = _uiState.value
        val form = state.registerForm ?: return
        if (!state.canRegisterDevices || !form.canSubmit || state.isSavingDeviceAction || !state.isOnline) return
        _uiState.update { it.copy(isSavingDeviceAction = true, error = null) }
        viewModelScope.launch {
            registerObdDevice(
                deviceIdentifier = form.deviceIdentifier,
                connectionType = form.connectionType,
                hardwareModel = form.hardwareModel,
                firmwareVersion = form.firmwareVersion
            )
                .onSuccess { created ->
                    _uiState.update {
                        it.copy(
                            isSavingDeviceAction = false,
                            registerForm = null,
                            devices = listOf(created) + it.devices,
                            notice = UiText.Resource(R.string.obd_registered_notice, created.deviceIdentifier)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSavingDeviceAction = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun onOpenStatusSheet(device: ObdDevice) {
        if (!_uiState.value.canRegisterDevices || !_uiState.value.isOnline) return
        _uiState.update { it.copy(editingStatusDevice = device, error = null, notice = null) }
    }

    fun onDismissStatusSheet() = _uiState.update {
        if (it.isSavingDeviceAction) it else it.copy(editingStatusDevice = null)
    }

    fun onUpdateDeviceStatus(status: DeviceStatus) {
        val device = _uiState.value.editingStatusDevice ?: return
        if (_uiState.value.isSavingDeviceAction) return
        _uiState.update { it.copy(isSavingDeviceAction = true, error = null) }
        viewModelScope.launch {
            updateObdDeviceStatus(device.id, status)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isSavingDeviceAction = false,
                            editingStatusDevice = null,
                            devices = state.devices.map { if (it.id == updated.id) updated else it },
                            notice = UiText.Resource(R.string.obd_status_updated_notice, updated.deviceIdentifier)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSavingDeviceAction = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun onOpenInstallDevice(device: ObdDevice) {
        if (!_uiState.value.canManageInstallations || !_uiState.value.isOnline) return
        _uiState.update { it.copy(installingDevice = device, error = null, notice = null) }
    }

    fun onDismissInstallDevice() = _uiState.update {
        if (it.isSavingDeviceAction) it else it.copy(installingDevice = null)
    }

    fun onConfirmInstallDevice(vehicle: VehicleWithOwner) {
        val device = _uiState.value.installingDevice ?: return
        if (_uiState.value.isSavingDeviceAction) return
        _uiState.update { it.copy(isSavingDeviceAction = true, error = null) }
        viewModelScope.launch {
            installObdDevice(device.id, vehicle.vehicle.id, vehicle.vehicle.currentMileage)
                .onSuccess { installation ->
                    val summary = DeviceInstallationSummary(
                        installationId = installation.id,
                        deviceId = device.id,
                        vehicleId = vehicle.vehicle.id,
                        plate = vehicle.vehicle.plate,
                        vehicleModel = "${vehicle.vehicle.brand} ${vehicle.vehicle.model}".trim(),
                        ownerName = vehicle.owner?.displayName
                    )
                    _uiState.update { state ->
                        state.copy(
                            isSavingDeviceAction = false,
                            installingDevice = null,
                            deviceInstallations = state.deviceInstallations + (device.id to summary),
                            activeInstallation = if (state.selectedVehicleId == vehicle.vehicle.id) installation else state.activeInstallation,
                            notice = UiText.Resource(R.string.obd_install_success, vehicle.vehicle.plate)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSavingDeviceAction = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun onOpenUninstallDevice(device: ObdDevice) {
        if (!_uiState.value.canManageInstallations || !_uiState.value.isOnline) return
        _uiState.update { it.copy(uninstallingDevice = device, error = null, notice = null) }
    }

    fun onDismissUninstallDevice() = _uiState.update {
        if (it.isSavingDeviceAction) it else it.copy(uninstallingDevice = null)
    }

    fun onConfirmUninstallDevice() {
        val device = _uiState.value.uninstallingDevice ?: return
        val summary = _uiState.value.deviceInstallations[device.id] ?: return
        if (_uiState.value.isSavingDeviceAction) return
        _uiState.update { it.copy(isSavingDeviceAction = true, error = null) }
        viewModelScope.launch {
            uninstallObdDevice(summary.installationId, null)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSavingDeviceAction = false,
                            uninstallingDevice = null,
                            deviceInstallations = state.deviceInstallations - device.id,
                            activeInstallation = if (state.selectedVehicleId == summary.vehicleId) null else state.activeInstallation,
                            notice = UiText.Resource(R.string.obd_uninstall_success)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSavingDeviceAction = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    // endregion

    // region Alertas predictivas (Tab 3)

    fun setAlertsScope(scope: AlertsScope) {
        _uiState.update { it.copy(alertsScope = scope, error = null) }
        refreshAlerts()
    }

    fun refreshAlerts() {
        if (!_uiState.value.canReadAlerts) return
        when (_uiState.value.alertsScope) {
            AlertsScope.TENANT -> loadTenantAlerts()
            AlertsScope.VEHICLE -> {
                val vehicleId = _uiState.value.selectedVehicleId ?: return
                _uiState.update { it.copy(isLoadingAlerts = true, error = null) }
                viewModelScope.launch {
                    refreshPredictiveAlerts(vehicleId)
                        .onSuccess { _uiState.update { it.copy(isLoadingAlerts = false) } }
                        .onFailure { e -> _uiState.update { it.copy(isLoadingAlerts = false, error = e.toDiagnosticsUiText()) } }
                }
            }
        }
    }

    private fun loadTenantAlerts() {
        if (!_uiState.value.canReadAlerts) return
        _uiState.update { it.copy(isLoadingAlerts = true, error = null) }
        viewModelScope.launch {
            getTenantPredictiveAlerts()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingAlerts = false, tenantAlerts = list) }
                    val cachedIds = _uiState.value.workshopVehicles.map { it.vehicle.id }.toSet()
                    val missing = list.map { it.vehicleId }.filter { it.isNotBlank() && it !in cachedIds }.distinct()
                    if (missing.isNotEmpty()) {
                        enrichReferences(missing, emptyList())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingAlerts = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        val state = _uiState.value
        if (!state.canAcknowledgeAlerts || !state.isOnline || state.isActingOnAlert) return
        _uiState.update { it.copy(isActingOnAlert = true, error = null, notice = null) }
        viewModelScope.launch {
            acknowledgePredictiveAlert(alertId)
                .onSuccess { updated ->
                    _uiState.update { current ->
                        current.copy(
                            isActingOnAlert = false,
                            tenantAlerts = current.tenantAlerts.map {
                                if (it.id == updated.id) updated.copy(status = updated.status ?: PredictiveAlertStatus.ACKNOWLEDGED) else it
                            },
                            notice = UiText.Resource(R.string.diag_alerts_ack_done)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isActingOnAlert = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun openConvertAlertSheet(alert: PredictiveAlert) {
        if (!_uiState.value.canCreateAppointment || !_uiState.value.isOnline) return
        val defaultDate = com.andeva.atelier.mobile.core.domain.BusinessTime.limaDate(java.time.Instant.now()).plusDays(1)
        _uiState.update {
            it.copy(
                convertingAlert = alert,
                convertAlertDateText = defaultDate.toString(),
                convertAlertTimeText = "10:00",
                convertAlertDurationText = "60",
                convertAlertReason = alert.message,
                error = null,
                notice = null
            )
        }
    }

    fun dismissConvertAlertSheet() = _uiState.update {
        if (it.isActingOnAlert) it else it.copy(convertingAlert = null, error = null)
    }

    fun onConvertAlertQuickDate(daysFromToday: Long) {
        val date = com.andeva.atelier.mobile.core.domain.BusinessTime.limaDate(java.time.Instant.now()).plusDays(daysFromToday)
        _uiState.update { it.copy(convertAlertDateText = date.toString(), error = null) }
    }

    fun onConvertAlertDateChange(value: String) = _uiState.update {
        it.copy(convertAlertDateText = value.take(10), error = null)
    }

    fun onConvertAlertTimeChange(value: String) = _uiState.update {
        it.copy(convertAlertTimeText = value.take(5), error = null)
    }

    fun onConvertAlertDurationChange(value: String) = _uiState.update {
        it.copy(convertAlertDurationText = value.filter(Char::isDigit).take(3), error = null)
    }

    fun onConvertAlertReasonChange(value: String) = _uiState.update {
        it.copy(convertAlertReason = value.take(500), error = null)
    }

    fun confirmConvertAlertToAppointment() {
        val state = _uiState.value
        val alert = state.convertingAlert ?: return
        if (!state.canCreateAppointment || !state.isOnline || state.isActingOnAlert) return

        val parsedDate = runCatching { java.time.LocalDate.parse(state.convertAlertDateText.trim()) }.getOrNull()
        val parsedTime = runCatching { java.time.LocalTime.parse(state.convertAlertTimeText.trim()) }.getOrNull()
        val scheduledAt = if (parsedDate != null && parsedTime != null) {
            java.time.ZonedDateTime.of(
                parsedDate,
                parsedTime,
                com.andeva.atelier.mobile.core.domain.BusinessTime.ZONE
            ).toInstant()
        } else null
        if (scheduledAt == null || !scheduledAt.isAfter(java.time.Instant.now())) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.diag_alerts_convert_invalid_datetime)) }
            return
        }
        val duration = state.convertAlertDurationText.trim().toIntOrNull()
        if (duration == null || duration < 15) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.diag_alerts_convert_invalid_duration)) }
            return
        }
        val reason = state.convertAlertReason.trim().ifBlank { alert.message }

        _uiState.update { it.copy(isActingOnAlert = true, error = null) }
        viewModelScope.launch {
            convertAlertToAppointment(
                alertId = alert.id,
                scheduledAt = scheduledAt,
                estimatedDurationMinutes = duration,
                reason = reason
            )
                .onSuccess { appointment ->
                    val dateText = (appointment.scheduledAt ?: scheduledAt).formatLimaDateTime()
                    _uiState.update { current ->
                        current.copy(
                            isActingOnAlert = false,
                            convertingAlert = null,
                            notice = UiText.Resource(R.string.diag_alerts_convert_ok, dateText.ifBlank { appointment.appointmentId })
                        )
                    }
                    refreshAlerts()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isActingOnAlert = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    // endregion
}
