package com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth

import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterUnreachableException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionLostException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNoResponseException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** ELM327 simulado: cada comando responde lo que diga [responses]; sin respuesta definida, `NO DATA`. */
class FakeObdTransport : ObdTransport {
    val responses = mutableMapOf(
        "ATZ" to "ELM327 v1.5\r\r>",
        "ATE0" to "OK\r\r>", "ATL0" to "OK\r\r>", "ATS0" to "OK\r\r>", "ATH0" to "OK\r\r>", "ATSP0" to "OK\r\r>",
        "0100" to "SEARCHING...\r4100BE3EB811\r\r>",
        "ATDPN" to "A6\r\r>",
        "0902" to "014\r0:490201314731\r1:4A433534343452\r2:37323532333637\r\r>"
    )
    val commands = mutableListOf<String>()
    var openError: Exception? = null
    var failOn: String? = null
    override var isOpen = false
        private set

    override suspend fun open(address: String) {
        openError?.let { throw it }
        isOpen = true
    }

    override suspend fun command(command: String, timeoutMillis: Long): String {
        if (!isOpen) throw IOException("Socket closed")
        commands += command
        if (command == failOn) throw IOException("Timeout")
        return responses[command] ?: "NO DATA\r\r>"
    }

    override fun close() {
        isOpen = false
    }
}

class Elm327ObdLinkTest {

    private val transport = FakeObdTransport()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00.123456Z"), ZoneOffset.UTC)
    private val link = Elm327ObdLink(transport, clock)

    @Test
    fun `inicializa el ELM327 y lee protocolo y VIN`() = runTest {
        val info = link.connect("AA:BB:CC:11:22:33").getOrThrow()

        assertEquals("ISO 15765-4 CAN (11 bit, 500 kbaud)", info.protocol)
        assertEquals("1G1JC5444R7252367", info.vin)
        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "0100", "ATDPN", "0902"), transport.commands)
        assertTrue(link.isConnected)
    }

    @Test
    fun `sin respuesta de la ECU falla y cierra el canal`() = runTest {
        transport.responses["0100"] = "SEARCHING...\rUNABLE TO CONNECT\r\r>"

        assertTrue(link.connect("AA:BB:CC:11:22:33").exceptionOrNull() is ObdNoResponseException)
        assertFalse(link.isConnected)
    }

    @Test
    fun `el adaptador que no contesta es inalcanzable y el VIN es opcional`() = runTest {
        transport.failOn = "ATZ"
        assertTrue(link.connect("AA").exceptionOrNull() is ObdAdapterUnreachableException)

        transport.failOn = "0902"
        val info = link.connect("AA").getOrThrow()
        assertNull(info.vin)
    }

    @Test
    fun `lee y borra codigos`() = runTest {
        link.connect("AA").getOrThrow()

        assertEquals(emptyList<Any>(), link.readDtcs().getOrThrow())
        transport.responses["03"] = "43020300 0133\r\r>"
        assertEquals(listOf("P0300", "P0133"), link.readDtcs().getOrThrow().map { it.code })

        transport.responses["04"] = "44\r\r>"
        assertTrue(link.clearDtcs().isSuccess)
        transport.responses["04"] = "UNABLE TO CONNECT\r\r>"
        assertTrue(link.clearDtcs().exceptionOrNull() is ObdNoResponseException)
    }

    @Test
    fun `cuadro congelado del codigo que encendio el testigo`() = runTest {
        link.connect("AA").getOrThrow()
        assertNull(link.readFreezeFrame().getOrThrow())

        transport.responses["0202"] = "4202000300\r>"
        transport.responses["020500"] = "4205007B\r>"
        transport.responses["020C00"] = "420C001AF8\r>"
        val frame = link.readFreezeFrame().getOrThrow()!!
        assertEquals("P0300", frame.dtc?.code)
        assertEquals(83.0, frame.engineTempCelsius!!, 0.0)
        assertEquals(1726, frame.engineRpm)
        assertNull(frame.speedKmh)
    }

    @Test
    fun `muestra con PID sin soporte y hora truncada a milisegundos`() = runTest {
        link.connect("AA").getOrThrow()
        transport.responses["0105"] = "41057B\r>"
        transport.responses["010C"] = "410C1AF8\r>"
        transport.responses["ATRV"] = "13.9V\r>"

        val sample = link.readSample().getOrThrow()
        assertEquals(Instant.parse("2026-10-24T14:00:00.123Z"), sample.timestamp)
        assertEquals(83.0, sample.engineTempCelsius!!, 0.0)
        assertEquals(1726, sample.engineRpm)
        assertEquals(13.9, sample.batteryVoltage!!, 0.0)
        assertNull(sample.speedKmh)
        assertNull(sample.fuelPercentage)
    }

    @Test
    fun `una caida del canal es conexion perdida y se puede reconectar`() = runTest {
        assertTrue(link.readSample().exceptionOrNull() is ObdNotConnectedException)
        assertTrue(link.reconnect().exceptionOrNull() is ObdNotConnectedException)

        link.connect("AA").getOrThrow()
        transport.failOn = "0105"
        assertTrue(link.readSample().exceptionOrNull() is ObdConnectionLostException)
        assertFalse(link.isConnected)

        transport.failOn = null
        assertTrue(link.reconnect().isSuccess)
        assertTrue(link.isConnected)
    }
}
