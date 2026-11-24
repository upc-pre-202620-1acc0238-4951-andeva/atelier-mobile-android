package com.andeva.atelier.mobile.features.diagnostics.presentation.live

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.diagnostics.application.FreezeTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetLatestVehicleTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetVehicleTelemetryHistoryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveDiagnosticsSyncStateUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ObserveLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.ReconnectObdUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.StartLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.StopLiveTelemetryUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.LatestVehicleTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.LiveTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryHistoryBucket
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySnapshot
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveVehicleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class LiveTelemetryViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<DiagnosticsRepository>(relaxed = true)
    private val diagnosticSession = mockk<DiagnosticSession>(relaxed = true)
    private val liveFlow = MutableStateFlow(LiveTelemetry())
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns MutableStateFlow(true) }
    private val clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)

    private fun createVm(permissions: List<Permission>): LiveTelemetryViewModel {
        val session = Session(
            "u1", "u@taller.pe", "Usuario", "t1", "Atelier", "m1",
            permissions.map { it.value }.toSet(),
            Plan("PRO", "ACTIVE", true, iotTelemetryEnabled = true, aiDiagnosticsEnabled = false)
        )
        val observeSession = mockk<ObserveSessionUseCase> { every { this@mockk.invoke() } returns flowOf(session) }
        val observeVehicle = mockk<ObserveVehicleUseCase> { every { this@mockk.invoke("v1") } returns flowOf(null) }
        every { diagnosticSession.state } returns liveFlow
        every { repository.observeSyncState("v1") } returns flowOf(DiagnosticsSyncState(5, 0))

        return LiveTelemetryViewModel(
            observeSession = observeSession,
            observeVehicle = observeVehicle,
            observeLiveTelemetry = ObserveLiveTelemetryUseCase(diagnosticSession),
            observeSyncState = ObserveDiagnosticsSyncStateUseCase(repository),
            connectivity = connectivity,
            startLiveTelemetry = StartLiveTelemetryUseCase(diagnosticSession),
            stopLiveTelemetry = StopLiveTelemetryUseCase(diagnosticSession),
            reconnectObd = ReconnectObdUseCase(diagnosticSession),
            freezeTelemetry = FreezeTelemetryUseCase(diagnosticSession),
            getLatestTelemetry = GetLatestVehicleTelemetryUseCase(repository),
            getTelemetryHistory = GetVehicleTelemetryHistoryUseCase(repository),
            clock = clock,
            savedStateHandle = SavedStateHandle(mapOf("vehicleId" to "v1", "plate" to "BKL482"))
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `sin bluetooth carga lectura remota automaticamente cuando tiene TELEMETRY_READ`() = runTest(mainRule.dispatcher) {
        val remote = LatestVehicleTelemetry("v1", Instant.parse("2026-10-24T13:50:00Z"), null, null, 60, 91.0, 2300, 13.9, 55.0)
        val bucket = TelemetryHistoryBucket(Instant.parse("2026-10-24T13:00:00Z"), "v1", 50, 89.0, 2100, 56.0, 13.9, 40)
        coEvery { repository.getLatestVehicleTelemetry("v1") } returns Result.success(remote)
        coEvery { repository.getVehicleTelemetryHistory("v1", any(), any(), "1 hour") } returns Result.success(listOf(bucket))

        val vm = createVm(listOf(Permission.TELEMETRY_READ))
        settle()

        assertTrue(vm.uiState.value.isRemoteMode)
        assertEquals(2300, vm.uiState.value.remoteTelemetry?.engineRpm)
        assertEquals(1, vm.uiState.value.telemetryHistory.size)
        assertEquals(5, vm.uiState.value.syncState.pendingReadings)
    }

    @Test
    fun `muestra sin PID 0105 activa aviso H23 y congelar abre snapshot`() = runTest(mainRule.dispatcher) {
        val noTempSample = TelemetrySample(Instant.EPOCH, null, 900, 0, 13.7, 50.0, null)
        liveFlow.value = LiveTelemetry(ObdLinkState.STREAMING, "v1", noTempSample)
        every { diagnosticSession.freeze() } returns TelemetrySnapshot("v1", noTempSample, emptySet())

        val vm = createVm(listOf(Permission.TELEMETRY_INGEST))
        settle()

        assertFalse(vm.uiState.value.isRemoteMode)
        assertTrue(vm.uiState.value.missingCoolantPid0105)

        vm.freezeSample()
        assertEquals(900, vm.uiState.value.frozenSnapshot?.sample?.engineRpm)

        vm.stopStreaming()
        settle()
        coVerify(exactly = 1) { diagnosticSession.stopStreaming() }
    }
}
