package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.diagnostics.domain.ClearFaultsResult
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.InsufficientTelemetryException
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.ObdForegroundController
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.session.FakeObdLink
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.session.LiveDiagnosticSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsUseCasesTest {

    private val link = FakeObdLink()
    private val repository = mockk<DiagnosticsRepository>(relaxed = true)
    private val healthReports = mockk<HealthReportRepository>()
    private val locationProvider = mockk<LocationProvider>(relaxed = true)

    private fun TestScope.session() =
        LiveDiagnosticSession(link, repository, locationProvider, mockk<ObdForegroundController>(relaxed = true), backgroundScope)

    private fun dtc(code: String) = Dtc.of(code)!!
    private fun fault(id: String, code: String) = VehicleFault(id, "v1", dtc(code), code, null, "", null, false, null)

    @Test
    fun `escanear sin conexion falla y sin codigos queda limpio`() = runTest {
        val session = session()
        assertTrue(ScanFaultsUseCase(session, repository)("v1").exceptionOrNull() is ObdNotConnectedException)

        session.connect("AA").getOrThrow()
        coEvery { repository.registerDetectedFaults("v1", emptyList(), any()) } returns emptyList()
        val result = ScanFaultsUseCase(session, repository)("v1").getOrThrow()

        assertTrue(result.isClean)
        assertNull(result.freezeFrame)
    }

    @Test
    fun `escanear registra los codigos con la sesion y trae el cuadro congelado`() = runTest {
        val session = session()
        session.connect("AA").getOrThrow()
        link.dtcs = listOf(dtc("P0304"))
        coEvery { repository.registerDetectedFaults("v1", link.dtcs, session.sessionId!!) } returns listOf(fault("local-1", "P0304"))

        val result = ScanFaultsUseCase(session, repository)("v1").getOrThrow()

        assertEquals("local-1", result.faults.single().id)
        assertEquals("P0304", result.freezeFrame?.dtc?.code)
    }

    @Test
    fun `borrar re-lee los codigos y pasa los que persisten`() = runTest {
        val session = session()
        session.connect("AA").getOrThrow()
        link.dtcs = listOf(dtc("P0304"), dtc("P0420"))
        link.dtcsAfterClear = listOf(dtc("P0420"))
        coEvery { repository.applyClearedFaults("v1", listOf(dtc("P0420")), any()) } returns
            ClearFaultsResult(listOf(fault("f1", "P0304")), listOf(fault("f2", "P0420")))

        val result = ClearFaultsUseCase(session, repository)("v1").getOrThrow()

        assertEquals(listOf("f2"), result.persistent.map { it.id })
        assertTrue(!result.isClean)
    }

    @Test
    fun `informe IA valida los dias y traduce el 422`() = runTest {
        val generate = GenerateHealthReportUseCase(healthReports)
        assertEquals("daysToAnalyze", (generate("v1", 5).exceptionOrNull() as InvalidDiagnosticsInputException).field)
        coVerify(exactly = 0) { healthReports.generate(any(), any(), any()) }

        coEvery { healthReports.generate("v1", 30, null) } returns
            Result.failure(ApiException(422, "ERR_INSUFFICIENT_TELEMETRY", "Telemetría insuficiente", emptyList()))
        val error = generate("v1", 30).exceptionOrNull() as InsufficientTelemetryException
        assertEquals("Telemetría insuficiente", error.message)

        coEvery { healthReports.generate("v1", 30, null) } returns
            Result.failure(ApiException(403, "QUOTA_EXCEEDED", "Cuota de IA agotada", emptyList()))
        assertEquals("Cuota de IA agotada", (generate("v1", 30).exceptionOrNull() as ApiException).message)
    }

    @Test
    fun `odometro negativo y congelar sin muestra`() = runTest {
        assertEquals(
            "currentOdometerKm",
            (InstallObdDeviceUseCase(repository)("d1", "v1", -5).exceptionOrNull() as InvalidDiagnosticsInputException).field
        )
        assertEquals(
            "finalOdometerKm",
            (UninstallObdDeviceUseCase(repository)("i1", -1).exceptionOrNull() as InvalidDiagnosticsInputException).field
        )
        assertTrue(FreezeTelemetryUseCase(session())().exceptionOrNull() is ObdNotConnectedException)
    }
}
