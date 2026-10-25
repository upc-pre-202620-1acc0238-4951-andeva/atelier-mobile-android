package com.andeva.atelier.mobile.features.diagnostics.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DiagnosticsDomainTest {

    private fun dtc(code: String) = Dtc.of(code)!!

    @Test
    fun `el DTC se normaliza y valida con el formato del backend`() {
        assertEquals("P0301", Dtc.of(" p0301 ")?.code)
        assertNull(Dtc.of("X1234"))
        assertNull(Dtc.of("P030"))
        assertTrue(Dtc.isValid("u0100"))
    }

    @Test
    fun `catalogo de DTC con severidad por codigo y por familia`() {
        assertEquals(FaultSeverity.CRITICAL, DtcCatalog.describe(dtc("P0217")).severity)
        assertEquals(FaultSeverity.CRITICAL, DtcCatalog.describe(dtc("P0304")).severity)
        assertTrue(DtcCatalog.describe(dtc("P0304")).description.endsWith("cilindro 4"))
        assertEquals(FaultSeverity.LOW, DtcCatalog.describe(dtc("P0456")).severity)
        assertEquals(FaultSeverity.LOW, DtcCatalog.describe(dtc("B1234")).severity)
        assertEquals(FaultSeverity.MEDIUM, DtcCatalog.describe(dtc("C0035")).severity)
        assertEquals("Falla de red de comunicación U0401", DtcCatalog.describe(dtc("U0401")).description)
    }

    @Test
    fun `umbrales de la telemetria en vivo`() {
        val sample = TelemetrySample(Instant.EPOCH, 110.0, 6500, 0, 11.5, null, null)
        assertEquals(
            setOf(TelemetryWarning.COOLANT_OVERHEAT, TelemetryWarning.HIGH_RPM, TelemetryWarning.LOW_VOLTAGE),
            TelemetryThresholds.warningsFor(sample)
        )
        assertTrue(TelemetryThresholds.warningsFor(sample.copy(engineTempCelsius = 90.0, engineRpm = 800, batteryVoltage = 13.9)).isEmpty())
        assertEquals(setOf(TelemetryWarning.HIGH_VOLTAGE), TelemetryThresholds.warningsFor(sample.copy(engineTempCelsius = null, engineRpm = null, batteryVoltage = 15.5)))
        assertFalse(sample.copy(engineTempCelsius = null).canBeSent)
    }

    @Test
    fun `validaciones y sugerencia de dispositivo por MAC`() {
        assertTrue(DiagnosticsValidation.isDaysToAnalyze(7))
        assertTrue(DiagnosticsValidation.isDaysToAnalyze(90))
        assertFalse(DiagnosticsValidation.isDaysToAnalyze(6))
        assertFalse(DiagnosticsValidation.isDaysToAnalyze(91))
        assertFalse(DiagnosticsValidation.isOdometer(-1))
        assertTrue(DiagnosticsValidation.isOdometer(null))

        val device = ObdDevice("d1", "aa:bb:cc:11:22:33", "BLUETOOTH_BLE", DeviceStatus.ACTIVE, null, null)
        assertTrue(device.matches("AA:BB:CC:11:22:33"))
        assertFalse(device.matches("AA:BB:CC:11:22:34"))
    }
}
