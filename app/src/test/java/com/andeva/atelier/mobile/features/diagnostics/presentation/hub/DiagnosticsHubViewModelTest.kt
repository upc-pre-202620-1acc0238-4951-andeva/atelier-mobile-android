package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
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
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothPermissionDeniedException
import com.andeva.atelier.mobile.features.diagnostics.domain.ConvertedAppointment
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.LiveTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapter
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterProvider
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNoResponseException
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertType
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.EnrichReceptionReferencesUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveWorkshopVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import com.andeva.atelier.mobile.features.tasks.application.RefreshMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskDao
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DiagnosticsHubViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<DiagnosticsRepository>(relaxed = true)
    private val diagnosticSession = mockk<DiagnosticSession>(relaxed = true)
    private val adapterProvider = mockk<ObdAdapterProvider>()
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns MutableStateFlow(true) }
    private val observeWorkshopVehicles = mockk<ObserveWorkshopVehiclesUseCase>()
    private val enrichReferences = mockk<EnrichReceptionReferencesUseCase>(relaxed = true)
    private val refreshMyTasks = mockk<RefreshMyTasksUseCase>(relaxed = true)
    private val refreshBranchWorkOrders = mockk<RefreshBranchWorkOrdersUseCase>(relaxed = true)
    private val taskDao = mockk<TaskDao>()
    private val workOrderDao = mockk<WorkOrderDao>()

    private val liveTelemetryFlow = MutableStateFlow(LiveTelemetry())
    private val vehicle = VehicleWithOwner(
        vehicle = Vehicle("v1", "BKL482", null, "Toyota", "Hilux", 2022, EngineType.DIESEL, "c1", 45000),
        owner = null
    )

    private fun session(
        permissions: List<Permission>,
        plan: Plan? = Plan("PRO", "ACTIVE", accessGranted = true, iotTelemetryEnabled = true, aiDiagnosticsEnabled = false)
    ) = Session(
        userId = "u1",
        email = "tec@taller.pe",
        fullName = "Andrés",
        tenantId = "t1",
        tenantName = "Atelier",
        membershipId = "m1",
        permissions = permissions.map { it.value }.toSet(),
        plan = plan,
        branchId = "b1"
    )

    private fun createVm(
        session: Session?,
        initialVehicleId: String? = null
    ): DiagnosticsHubViewModel {
        val observeSession = mockk<ObserveSessionUseCase> { every { this@mockk.invoke() } returns flowOf(session) }
        val observePending = mockk<ObservePendingOperationCountUseCase> { every { this@mockk.invoke() } returns flowOf(0) }
        every { observeWorkshopVehicles() } returns flowOf(listOf(vehicle))
        every { taskDao.observeAll() } returns flowOf(emptyList())
        every { workOrderDao.observeBranch("b1") } returns flowOf(emptyList())
        every { diagnosticSession.state } returns liveTelemetryFlow
        every { repository.observeSyncState(any()) } returns flowOf(DiagnosticsSyncState(0, 0))
        every { repository.observePredictiveAlerts(any()) } returns flowOf(emptyList())

        return DiagnosticsHubViewModel(
            observeSession = observeSession,
            observePendingCount = observePending,
            connectivity = connectivity,
            pushInbox = PushInbox(),
            observeWorkshopVehicles = observeWorkshopVehicles,
            enrichReferences = enrichReferences,
            refreshMyTasks = refreshMyTasks,
            refreshBranchWorkOrders = refreshBranchWorkOrders,
            taskDao = taskDao,
            workOrderDao = workOrderDao,
            getActiveInstallation = GetActiveInstallationUseCase(repository),
            installObdDevice = InstallObdDeviceUseCase(repository),
            uninstallObdDevice = UninstallObdDeviceUseCase(repository),
            getObdDevices = GetObdDevicesUseCase(repository),
            registerObdDevice = RegisterObdDeviceUseCase(repository),
            updateObdDeviceStatus = UpdateObdDeviceStatusUseCase(repository),
            getPairedObdAdapters = GetPairedObdAdaptersUseCase(adapterProvider),
            connectObd = ConnectObdUseCase(diagnosticSession),
            disconnectObd = DisconnectObdUseCase(diagnosticSession),
            observeLiveTelemetry = ObserveLiveTelemetryUseCase(diagnosticSession),
            observeDiagnosticsSyncState = ObserveDiagnosticsSyncStateUseCase(repository),
            getTenantPredictiveAlerts = GetTenantPredictiveAlertsUseCase(repository),
            refreshPredictiveAlerts = RefreshPredictiveAlertsUseCase(repository),
            observePredictiveAlerts = ObservePredictiveAlertsUseCase(repository),
            acknowledgePredictiveAlert = AcknowledgePredictiveAlertUseCase(repository),
            convertAlertToAppointment = ConvertAlertToAppointmentUseCase(repository),
            savedStateHandle = SavedStateHandle(mapOf("initialVehicleId" to initialVehicleId))
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `plan sin telemetria bloquea el hub`() = runTest(mainRule.dispatcher) {
        every { adapterProvider.pairedAdapters() } returns Result.success(emptyList())
        val planGo = Plan("GO", "ACTIVE", accessGranted = true, iotTelemetryEnabled = false, aiDiagnosticsEnabled = false)
        val vm = createVm(session(listOf(Permission.TELEMETRY_INGEST, Permission.DEVICES_READ), plan = planGo))
        settle()

        assertFalse(vm.uiState.value.planIncludesTelemetry)
        assertFalse(vm.uiState.value.canUseTelemetry)
    }

    @Test
    fun `permiso bluetooth denegado y luego concedido carga adaptadores y sugiere coincidencia`() = runTest(mainRule.dispatcher) {
        every { adapterProvider.pairedAdapters() } returnsMany listOf(
            Result.failure(BluetoothPermissionDeniedException()),
            Result.success(listOf(ObdAdapter("OBD-1", "AA:BB:CC:11:22:33")))
        )
        coEvery { repository.getDevices(any()) } returns Result.success(
            listOf(ObdDevice("d1", "aa:bb:cc:11:22:33", "BLUETOOTH", DeviceStatus.ACTIVE, "ELM327", null))
        )
        coEvery { repository.getActiveInstallation("v1") } returns Result.success(null)

        val vm = createVm(session(listOf(Permission.TELEMETRY_INGEST, Permission.DEVICES_READ)))
        settle()
        assertTrue(vm.uiState.value.bluetoothPermissionRequired)

        vm.onBluetoothPermissionResult(true)
        settle()
        assertFalse(vm.uiState.value.bluetoothPermissionRequired)
        assertEquals("AA:BB:CC:11:22:33", vm.uiState.value.selectedAdapterAddress)
    }

    @Test
    fun `conectar adaptador guarda info o muestra error si ECU no responde`() = runTest(mainRule.dispatcher) {
        every { adapterProvider.pairedAdapters() } returns Result.success(listOf(ObdAdapter("ELM", "AA:BB")))
        coEvery { diagnosticSession.connect("AA:BB") } returnsMany listOf(
            Result.failure(ObdNoResponseException()),
            Result.success(ObdConnectionInfo("ISO 15765-4 CAN", "1HGCM82633A004352"))
        )

        val vm = createVm(session(listOf(Permission.TELEMETRY_INGEST)))
        settle()

        vm.connectSelectedAdapter()
        settle()
        assertNotNull(vm.uiState.value.error)

        vm.connectSelectedAdapter()
        settle()
        assertEquals("ISO 15765-4 CAN", vm.uiState.value.connectionInfo?.protocol)
    }

    @Test
    fun `alertas del taller confirman lectura y convierten en cita`() = runTest(mainRule.dispatcher) {
        every { adapterProvider.pairedAdapters() } returns Result.success(emptyList())
        val alert = PredictiveAlert(
            "a1", "v1", PredictiveAlertType.ENGINE_OVERHEATING_RISK, 0.9, "Temp alta",
            PredictiveAlertStatus.DISPATCHED, null, Instant.parse("2026-10-24T14:00:00Z")
        )
        coEvery { repository.getTenantPredictiveAlerts() } returns Result.success(listOf(alert))
        coEvery { repository.acknowledgePredictiveAlert("a1") } returns Result.success(
            alert.copy(status = PredictiveAlertStatus.ACKNOWLEDGED)
        )
        coEvery { repository.convertAlertToAppointment("a1", any(), any(), any()) } returns Result.success(
            ConvertedAppointment("apt1", "v1", "c1", Instant.parse("2026-10-25T14:00:00Z"), "PENDING", null)
        )

        val vm = createVm(
            session(listOf(Permission.PREDICTIVE_ALERTS_READ, Permission.PREDICTIVE_ALERTS_ACKNOWLEDGE, Permission.APPOINTMENTS_CREATE))
        )
        settle()
        assertEquals(1, vm.uiState.value.tenantAlerts.size)

        vm.acknowledgeAlert("a1")
        settle()
        assertEquals(PredictiveAlertStatus.ACKNOWLEDGED, vm.uiState.value.tenantAlerts.single().status)

        vm.openConvertAlertSheet(alert)
        vm.confirmConvertAlertToAppointment()
        settle()
        coVerify(exactly = 1) { repository.convertAlertToAppointment("a1", any(), 60, "Temp alta") }
        assertNotNull(vm.uiState.value.notice)
    }

    @Test
    fun `instalar escaner valida odometro y actualiza instalacion activa`() = runTest(mainRule.dispatcher) {
        every { adapterProvider.pairedAdapters() } returns Result.success(emptyList())
        val dev = ObdDevice("d1", "AA:BB", "BLUETOOTH", DeviceStatus.ACTIVE, null, null)
        coEvery { repository.getDevices(any()) } returns Result.success(listOf(dev))
        coEvery { repository.getActiveInstallation("v1") } returns Result.success(null)
        coEvery { repository.installDevice("d1", "v1", 45000) } returns Result.success(
            DeviceInstallation("i1", "d1", "v1", Instant.EPOCH, null, 45000, null, true)
        )

        val vm = createVm(session(listOf(Permission.TELEMETRY_INGEST, Permission.DEVICES_READ, Permission.INSTALLATIONS_MANAGE)))
        settle()
        vm.openInstallForSelectedVehicle()
        vm.onInstallOdometerChange("45000")
        vm.confirmInstallOnSelectedVehicle(dev)
        settle()

        assertEquals("i1", vm.uiState.value.activeInstallation?.id)
    }
}
