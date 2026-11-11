package com.andeva.atelier.mobile.features.main.presentation.obd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.application.GetActiveInstallationUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetObdDevicesUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.InstallObdDeviceUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.RegisterObdDeviceUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.UninstallObdDeviceUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.UpdateObdDeviceStatusUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
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
class ObdViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getDevices: GetObdDevicesUseCase,
    private val registerDevice: RegisterObdDeviceUseCase,
    private val updateStatus: UpdateObdDeviceStatusUseCase,
    private val observeWorkshopVehicles: ObserveWorkshopVehiclesUseCase,
    private val getActiveInstallation: GetActiveInstallationUseCase,
    private val installDevice: InstallObdDeviceUseCase,
    private val uninstallDevice: UninstallObdDeviceUseCase,
    private val refreshMyTasks: RefreshMyTasksUseCase,
    private val refreshBranchWorkOrders: RefreshBranchWorkOrdersUseCase,
    private val enrichReferences: EnrichReceptionReferencesUseCase,
    private val taskDao: TaskDao,
    private val workOrderDao: WorkOrderDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObdUiState())
    val uiState: StateFlow<ObdUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canReadDevices = session?.has(Permission.OBD_DEVICES_READ) == true,
                        canRegisterDevices = session?.has(Permission.OBD_DEVICES_REGISTER) == true,
                        canManageInstallations = session?.has(Permission.INSTALLATIONS_MANAGE) == true ||
                            session?.has(Permission.OBD_DEVICES_REGISTER) == true
                    )
                }
                if (session != null) {
                    if (session.has(Permission.TASKS_READ)) {
                        launch { refreshMyTasks() }
                    }
                    if (session.has(Permission.WORK_ORDERS_READ)) {
                        launch { refreshBranchWorkOrders() }
                    }
                }
            }
        }
        viewModelScope.launch {
            observeSession().flatMapLatest { session ->
                val branchId = session?.branchId.orEmpty()
                val workOrdersFlow = if (branchId.isNotBlank()) {
                    workOrderDao.observeBranch(branchId)
                } else {
                    flowOf(emptyList())
                }
                combine(
                    observeWorkshopVehicles(),
                    taskDao.observeAll(),
                    workOrdersFlow
                ) { vehicles, tasks, workOrders ->
                    val cachedVehicleIds = vehicles.map { it.vehicle.id }.toSet()
                    val taskVehicles = tasks.map { it.task.vehicleId }.filter { it.isNotBlank() }
                    val orderVehicles = workOrders.map { it.workOrder.vehicleId }.filter { it.isNotBlank() }
                    val allNeededVehicles = (taskVehicles + orderVehicles).distinct()
                    val missingVehicles = allNeededVehicles.filter { it !in cachedVehicleIds }
                    if (missingVehicles.isNotEmpty()) {
                        launch { enrichReferences(missingVehicles, emptyList()) }
                    }
                    vehicles
                }
            }.collect { vehicles ->
                _uiState.update { it.copy(workshopVehicles = vehicles) }
                loadInstallations(vehicles)
            }
        }
        loadDevices()
    }

    private fun loadInstallations(vehicles: List<VehicleWithOwner>) {
        if (vehicles.isEmpty()) return
        viewModelScope.launch {
            val results = coroutineScope {
                vehicles.map { v ->
                    async {
                        val active = getActiveInstallation(v.vehicle.id).getOrNull()
                        if (active != null && active.isActive) {
                            active.deviceId to DeviceInstallationInfo(
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
            _uiState.update { it.copy(deviceInstallations = results) }
        }
    }

    fun loadDevices(filter: DeviceStatus? = _uiState.value.statusFilter) {
        _uiState.update { it.copy(isLoading = true, statusFilter = filter, error = null) }
        viewModelScope.launch {
            launch { refreshMyTasks() }
            launch { refreshBranchWorkOrders() }
            getDevices(status = filter)
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, devices = list) }
                    loadInstallations(_uiState.value.workshopVehicles)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUiText()) }
                }
        }
    }

    fun onOpenInstall(device: ObdDevice) {
        if (!_uiState.value.canManageInstallations) return
        _uiState.update { it.copy(installingDevice = device, error = null, notice = null) }
    }

    fun onDismissInstall() = _uiState.update {
        if (it.isSaving) it else it.copy(installingDevice = null)
    }

    fun onConfirmInstall(vehicle: VehicleWithOwner) {
        val device = _uiState.value.installingDevice ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            installDevice(
                deviceId = device.id,
                vehicleId = vehicle.vehicle.id,
                currentOdometerKm = vehicle.vehicle.currentMileage
            )
                .onSuccess { installation ->
                    val newInfo = DeviceInstallationInfo(
                        installationId = installation.id,
                        deviceId = device.id,
                        vehicleId = vehicle.vehicle.id,
                        plate = vehicle.vehicle.plate,
                        vehicleModel = "${vehicle.vehicle.brand} ${vehicle.vehicle.model}".trim(),
                        ownerName = vehicle.owner?.displayName
                    )
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            installingDevice = null,
                            deviceInstallations = state.deviceInstallations + (device.id to newInfo),
                            notice = UiText.Resource(R.string.obd_install_success, vehicle.vehicle.plate)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.toUiText()) }
                }
        }
    }

    fun onOpenUninstall(device: ObdDevice) {
        if (!_uiState.value.canManageInstallations) return
        _uiState.update { it.copy(uninstallingDevice = device, error = null, notice = null) }
    }

    fun onDismissUninstall() = _uiState.update {
        if (it.isSaving) it else it.copy(uninstallingDevice = null)
    }

    fun onConfirmUninstall() {
        val device = _uiState.value.uninstallingDevice ?: return
        val installation = _uiState.value.deviceInstallations[device.id] ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            uninstallDevice(installation.installationId, null)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            uninstallingDevice = null,
                            deviceInstallations = state.deviceInstallations - device.id,
                            notice = UiText.Resource(R.string.obd_uninstall_success)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.toUiText()) }
                }
        }
    }

    fun onOpenRegister() {
        if (!_uiState.value.canRegisterDevices) return
        _uiState.update { it.copy(registerForm = RegisterObdForm(), error = null, notice = null) }
    }

    fun onDismissRegister() = _uiState.update {
        if (it.isSaving) it else it.copy(registerForm = null, error = null)
    }

    fun onIdentifierChange(value: String) = editForm { it.copy(deviceIdentifier = value) }
    fun onConnectionTypeChange(value: String) = editForm { it.copy(connectionType = value) }
    fun onHardwareModelChange(value: String) = editForm { it.copy(hardwareModel = value) }
    fun onFirmwareVersionChange(value: String) = editForm { it.copy(firmwareVersion = value) }

    private fun editForm(change: (RegisterObdForm) -> RegisterObdForm) = _uiState.update {
        val form = it.registerForm ?: return@update it
        if (it.isSaving) it else it.copy(registerForm = change(form), error = null)
    }

    fun onConfirmRegister() {
        val state = _uiState.value
        val form = state.registerForm ?: return
        if (!state.canRegisterDevices || !form.canSubmit || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            registerDevice(
                deviceIdentifier = form.deviceIdentifier,
                connectionType = form.connectionType,
                hardwareModel = form.hardwareModel,
                firmwareVersion = form.firmwareVersion
            )
                .onSuccess { created ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            registerForm = null,
                            devices = listOf(created) + it.devices,
                            notice = UiText.Resource(R.string.obd_registered_notice, created.deviceIdentifier)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.toUiText()) }
                }
        }
    }

    fun onOpenStatusDialog(device: ObdDevice) {
        if (!_uiState.value.canRegisterDevices) return
        _uiState.update { it.copy(editingDevice = device, error = null, notice = null) }
    }

    fun onDismissStatusDialog() = _uiState.update {
        if (it.isSaving) it else it.copy(editingDevice = null)
    }

    fun onUpdateStatus(status: DeviceStatus) {
        val device = _uiState.value.editingDevice ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            updateStatus(device.id, status)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            editingDevice = null,
                            devices = state.devices.map { if (it.id == updated.id) updated else it },
                            notice = UiText.Resource(R.string.obd_status_updated_notice, updated.deviceIdentifier)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.toUiText()) }
                }
        }
    }
}
