package com.andeva.atelier.mobile.features.diagnostics.infrastructure.session

import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.FreezeFrame
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionLostException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLink
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNoResponseException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryWarning
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.ObdForegroundController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Vehículo simulado: [samples] responde en orden; con [lost] el canal se cae; [reconnects] dice si vuelve. */
class FakeObdLink : ObdLink {
    var connected = false
    var connectResult: Result<ObdConnectionInfo> = Result.success(ObdConnectionInfo("ISO 15765-4 CAN (11 bit, 500 kbaud)", "1G1JC5444R7252367"))
    var dtcs: List<Dtc> = emptyList()
    var dtcsAfterClear: List<Dtc> = emptyList()
    var lost = false
    var reconnects = false
    var reconnectAttempts = 0
    var sampleCount = 0

    override val isConnected get() = connected

    override suspend fun connect(address: String) = connectResult.also { connected = it.isSuccess }

    override suspend fun reconnect(): Result<ObdConnectionInfo> {
        reconnectAttempts++
        if (!reconnects) return Result.failure(ObdNoResponseException())
        connected = true
        lost = false
        return connectResult
    }

    override suspend fun readDtcs() = Result.success(dtcs)

    override suspend fun readFreezeFrame() = Result.success<FreezeFrame?>(FreezeFrame(dtcs.firstOrNull(), 92.0, 2100, 40))

    override suspend fun clearDtcs(): Result<Unit> {
        dtcs = dtcsAfterClear
        return Result.success(Unit)
    }

    override suspend fun readSample(): Result<TelemetrySample> {
        if (lost) {
            connected = false
            return Result.failure(ObdConnectionLostException())
        }
        sampleCount++
        return Result.success(TelemetrySample(Instant.EPOCH.plusSeconds(sampleCount * 2L), 110.0, 800, 0, 13.9, 50.0, 35))
    }

    override fun close() {
        connected = false
    }
}

class LiveDiagnosticSessionTest {

    private val link = FakeObdLink()
    private val repository = mockk<DiagnosticsRepository>(relaxed = true)
    private val foreground = mockk<ObdForegroundController>(relaxed = true)
    private val locationProvider = object : LocationProvider {
        override suspend fun currentLocation() = Result.success(DeviceLocation(-12.0464, -77.0428, 10f, false))
    }

    private fun TestScope.session() = LiveDiagnosticSession(link, repository, locationProvider, foreground, backgroundScope)

    @Test
    fun `conectar abre una sesion con su id`() = runTest {
        val session = session()
        assertNull(session.sessionId)

        session.connect("AA").getOrThrow()

        assertEquals(ObdLinkState.CONNECTED, session.state.value.state)
        assertNotNull(session.sessionId)
    }

    @Test
    fun `sin respuesta de la ECU la sesion queda desconectada`() = runTest {
        link.connectResult = Result.failure(ObdNoResponseException())
        val session = session()

        assertTrue(session.connect("AA").exceptionOrNull() is ObdNoResponseException)
        assertEquals(ObdLinkState.DISCONNECTED, session.state.value.state)
        assertTrue(session.startStreaming("v1").exceptionOrNull() is ObdNotConnectedException)
        assertTrue(session.withLink { Result.success(1) }.exceptionOrNull() is ObdNotConnectedException)
    }

    @Test
    fun `el muestreo guarda cada 2 s, arma lotes completos y avisa por umbral`() = runTest {
        val session = session()
        session.connect("AA").getOrThrow()

        session.startStreaming("v1").getOrThrow()
        runCurrent()
        advanceTimeBy(4_100)

        assertEquals(3, link.sampleCount)
        coVerify(exactly = 3) { repository.saveReading("v1", any(), any(), any()) }
        // La ubicación llega después de la primera muestra: no la bloquea
        coVerify(exactly = 2) { repository.saveReading("v1", any(), -12.0464, -77.0428) }
        coVerify(exactly = 3) { repository.enqueueUnbatchedReadings(onlyFull = true) }
        coVerify { repository.purgeConfirmedReadings() }
        verify { foreground.start() }
        assertEquals(setOf(TelemetryWarning.COOLANT_OVERHEAT), session.state.value.warnings)
        assertEquals("v1", session.freeze()?.vehicleId)

        session.stopStreaming()
        coVerify(exactly = 2) { repository.enqueueUnbatchedReadings(onlyFull = false) }     // al iniciar y al cerrar
        verify { foreground.stop() }
        assertEquals(ObdLinkState.CONNECTED, session.state.value.state)
    }

    @Test
    fun `una caida de senal se recupera si el adaptador vuelve`() = runTest {
        val session = session()
        session.connect("AA").getOrThrow()
        session.startStreaming("v1").getOrThrow()
        runCurrent()

        link.lost = true
        link.reconnects = true
        advanceTimeBy(4_100)

        assertEquals(1, link.reconnectAttempts)
        assertEquals(ObdLinkState.STREAMING, session.state.value.state)
        session.stopStreaming()
    }

    @Test
    fun `si no vuelve tras 3 intentos queda perdida y encola lo capturado`() = runTest {
        val session = session()
        session.connect("AA").getOrThrow()
        session.startStreaming("v1").getOrThrow()
        runCurrent()

        link.lost = true
        advanceTimeBy(20_000)

        assertEquals(LiveDiagnosticSession.MAX_RECONNECT_ATTEMPTS, link.reconnectAttempts)
        assertEquals(ObdLinkState.LOST, session.state.value.state)
        coVerify(exactly = 2) { repository.enqueueUnbatchedReadings(onlyFull = false) }
        verify { foreground.stop() }
    }

    @Test
    fun `desconectar cierra el canal y olvida la sesion`() = runTest {
        val session = session()
        session.connect("AA").getOrThrow()

        session.disconnect()

        assertNull(session.sessionId)
        assertEquals(ObdLinkState.DISCONNECTED, session.state.value.state)
        assertTrue(!link.connected)
        assertNull(session.freeze())
    }

    @Test
    fun `el muestreo y los escaneos se turnan el canal`() = runTest {
        coEvery { repository.saveReading(any(), any(), any(), any()) } returns true
        val session = session()
        session.connect("AA").getOrThrow()

        assertEquals(1, session.withLink { Result.success(1) }.getOrThrow())
    }
}
