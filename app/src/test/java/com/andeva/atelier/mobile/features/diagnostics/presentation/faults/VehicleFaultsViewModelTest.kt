package com.andeva.atelier.mobile.features.diagnostics.presentation.faults

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.diagnostics.application.ClearFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveDiagnosticsSyncStateUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveVehicleFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.RefreshVehicleFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ScanFaultsUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.ClearFaultsResult
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.FaultSeverity
import com.andeva.atelier.mobile.features.diagnostics.domain.LiveTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLink
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveVehicleUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class VehicleFaultsViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<DiagnosticsRepository>(relaxed = true)
    private val link = mockk<ObdLink>()
    private val session = mockk<DiagnosticSession>()
    private val faultsFlow = MutableStateFlow<List<VehicleFault>>(emptyList())
    private val liveFlow = MutableStateFlow(LiveTelemetry(state = ObdLinkState.CONNECTED))
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns MutableStateFlow(true) }

    private fun fault(id: String, code: String) = VehicleFault(
        id, "v1", Dtc.of(code), code, FaultSeverity.CRITICAL, "Falla $code", Instant.EPOCH, false, null
    )

    private fun createVm(): VehicleFaultsViewModel {
        val userSession = Session(
            "u1", "tec@taller.pe", "Andrés", "t1", "Atelier", "m1",
            setOf(Permission.TELEMETRY_INGEST.value, Permission.FAULTS_READ.value, Permission.FAULTS_RESOLVE.value),
            Plan("PRO", "ACTIVE", true, iotTelemetryEnabled = true, aiDiagnosticsEnabled = false)
        )
        val observeSession = mockk<ObserveSessionUseCase> { every { this@mockk.invoke() } returns flowOf(userSession) }
        val observeVehicle = mockk<ObserveVehicleUseCase> { every { this@mockk.invoke("v1") } returns flowOf(null) }
        every { repository.observeActiveFaults("v1") } returns faultsFlow
        every { repository.observeSyncState("v1") } returns flowOf(DiagnosticsSyncState(0, 1))
        every { session.state } returns liveFlow
        every { session.sessionId } returns "s1"

        return VehicleFaultsViewModel(
            observeSession = observeSession,
            observeVehicle = observeVehicle,
            observeVehicleFaults = ObserveVehicleFaultsUseCase(repository),
            observeSyncState = ObserveDiagnosticsSyncStateUseCase(repository),
            observeLiveTelemetry = ObserveLiveTelemetryUseCase(session),
            connectivity = connectivity,
            refreshVehicleFaults = RefreshVehicleFaultsUseCase(repository),
            scanFaultsUseCase = ScanFaultsUseCase(session, repository),
            clearFaultsUseCase = ClearFaultsUseCase(session, repository),
            savedStateHandle = SavedStateHandle(mapOf("vehicleId" to "v1", "plate" to "BKL482"))
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `escanear y borrar codigos actualiza el estado`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshActiveFaults("v1") } returns Result.success(Unit)
        coEvery { session.withLink<Pair<List<Dtc>, com.andeva.atelier.mobile.features.diagnostics.domain.FreezeFrame?>>(any()) } coAnswers {
            val block = firstArg<suspend (ObdLink) -> Result<Pair<List<Dtc>, com.andeva.atelier.mobile.features.diagnostics.domain.FreezeFrame?>>>()
            coEvery { link.readDtcs() } returns Result.success(listOf(Dtc.of("P0304")!!))
            coEvery { link.readFreezeFrame() } returns Result.success(null)
            block(link)
        }
        val scanned = fault("local-1", "P0304")
        coEvery { repository.registerDetectedFaults("v1", listOf(Dtc.of("P0304")!!), "s1") } returns listOf(scanned)

        val vm = createVm()
        settle()

        vm.scanDtcs()
        faultsFlow.value = listOf(scanned)
        settle()
        assertEquals(1, vm.uiState.value.faults.size)

        coEvery { session.withLink<List<Dtc>>(any()) } coAnswers {
            val block = firstArg<suspend (ObdLink) -> Result<List<Dtc>>>()
            coEvery { link.clearDtcs() } returns Result.success(Unit)
            coEvery { link.readDtcs() } returns Result.success(emptyList())
            block(link)
        }
        coEvery { repository.applyClearedFaults("v1", emptyList(), "s1") } returns ClearFaultsResult(listOf(scanned), emptyList())

        vm.openClearConfirm()
        assertTrue(vm.uiState.value.confirmClearOpen)
        vm.confirmClearDtcs()
        faultsFlow.value = emptyList()
        settle()

        assertFalse(vm.uiState.value.confirmClearOpen)
        assertTrue(vm.uiState.value.lastClearResult?.isClean == true)
    }
}
