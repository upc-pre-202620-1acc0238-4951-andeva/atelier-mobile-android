package com.andeva.atelier.mobile.features.diagnostics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdResponseParserTest {

    private fun codes(raw: String) = ObdResponseParser.dtcs(raw).map { it.code }

    @Test
    fun `modo 03 sin CAN trae tres pares por linea y rellena con ceros`() {
        assertEquals(listOf("P0133"), codes("43 01 33 00 00 00 00\r\r>"))
        assertEquals(listOf("P0133", "P0300", "C0035"), codes("43013303004035\r"))
    }

    @Test
    fun `modo 03 en CAN antepone la cantidad de codigos`() {
        assertEquals(listOf("P0133", "B0134"), codes("43 02 01 33 81 34"))
        assertEquals(emptyList<String>(), codes("4300"))
    }

    @Test
    fun `modo 03 en varias tramas CAN se une y respeta la cantidad`() {
        val raw = "00A\r0:430401330134\r1:01350136000000\r\r>"
        assertEquals(listOf("P0133", "P0134", "P0135", "P0136"), codes(raw))
    }

    @Test
    fun `decodifica las cuatro familias SAE`() {
        assertEquals("P0300", ObdResponseParser.decodeDtc(0x03, 0x00).code)
        assertEquals("C0035", ObdResponseParser.decodeDtc(0x40, 0x35).code)
        assertEquals("B1234", ObdResponseParser.decodeDtc(0x92, 0x34).code)
        assertEquals("U0100", ObdResponseParser.decodeDtc(0xC1, 0x00).code)
        assertEquals("P2A0F", ObdResponseParser.decodeDtc(0x2A, 0x0F).code)
    }

    @Test
    fun `PIDs del modo 01`() {
        assertEquals(83.0, ObdResponseParser.coolantTemp("41 05 7B")!!, 0.0)
        assertEquals(1726, ObdResponseParser.engineRpm("410C1AF8"))
        assertEquals(50, ObdResponseParser.speed("SEARCHING...\r410D32\r"))
        assertEquals(100.0, ObdResponseParser.fuelLevel("412FFF")!!, 0.001)
        assertEquals(101, ObdResponseParser.intakePressure("410B65"))
        assertEquals(12.6, ObdResponseParser.voltage("12.6V\r\r>")!!, 0.0)
        assertNull(ObdResponseParser.coolantTemp("NO DATA"))
        assertNull(ObdResponseParser.engineRpm("410C1A"))
    }

    @Test
    fun `VIN en varias tramas CAN`() {
        val raw = "014\r0:490201314731\r1:4A433534343452\r2:37323532333637\r>"
        assertEquals("1G1JC5444R7252367", ObdResponseParser.vin(raw))
        assertNull(ObdResponseParser.vin("NO DATA"))
    }

    @Test
    fun `VIN sin CAN llega en cinco lineas con secuencia`() {
        val raw = listOf("49020100000031", "49020247314A43", "49020335343434", "49020452373235", "49020532333637")
            .joinToString("\r")
        assertEquals("1G1JC5444R7252367", ObdResponseParser.vin(raw))
    }

    @Test
    fun `protocolo y respuestas sin la ECU`() {
        assertEquals("ISO 15765-4 CAN (11 bit, 500 kbaud)", ObdResponseParser.protocol("A6\r>"))
        assertEquals("ISO 9141-2", ObdResponseParser.protocol("3"))
        assertTrue(ObdResponseParser.isNoResponse("SEARCHING...\rUNABLE TO CONNECT\r>"))
        assertTrue(ObdResponseParser.isNoResponse("BUS INIT: ...ERROR"))
        assertTrue(ObdResponseParser.isNoResponse("?"))
        assertTrue(ObdResponseParser.isNoResponse(""))
        assertFalse(ObdResponseParser.isNoResponse("SEARCHING...\r41 00 BE 3E B8 11"))
        assertTrue(ObdResponseParser.isNoData("NO DATA\r>"))
        assertTrue(ObdResponseParser.isOk("OK\r>"))
    }

    @Test
    fun `cuadro congelado del modo 02`() {
        assertEquals("P0300", ObdResponseParser.freezeFrameDtc("42 02 00 03 00")?.code)
        assertNull(ObdResponseParser.freezeFrameDtc("4202000000"))
        assertEquals(listOf(0x7B), ObdResponseParser.freezeFrameData("4205007B", "05"))
    }
}
