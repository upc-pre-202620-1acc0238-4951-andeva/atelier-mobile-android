package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.ConvertedAppointment
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.LatestVehicleTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdResponseParser
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertType
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryHistoryBucket
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DiagnosticsPhase3UseCasesTest {

    private val repository = mockk<DiagnosticsRepository>()

    @Test
    fun `GetTenantPredictiveAlertsUseCase delega en el repositorio`() = runTest {
        val alerts = listOf(
            PredictiveAlert(
                id = "a1",
                vehicleId = "v1",
                alertType = PredictiveAlertType.BATTERY_FAILURE_RISK,
                confidenceScore = 0.85,
                message = "Voltaje bajo en arranque",
                status = PredictiveAlertStatus.DISPATCHED,
                recommendedServiceId = null,
                createdAt = Instant.parse("2026-10-24T10:00:00Z")
            )
        )
        coEvery { repository.getTenantPredictiveAlerts() } returns Result.success(alerts)

        val result = GetTenantPredictiveAlertsUseCase(repository)().getOrThrow()
        assertEquals(alerts, result)
    }

    @Test
    fun `GetLatestVehicleTelemetryUseCase y GetVehicleTelemetryHistoryUseCase delegan parametros`() = runTest {
        val latest = LatestVehicleTelemetry("v1", Instant.EPOCH, null, null, 50, 91.0, 2200, 14.0, 70.0)
        coEvery { repository.getLatestVehicleTelemetry("v1") } returns Result.success(latest)
        coEvery { repository.getLatestVehicleTelemetry("v2") } returns Result.success(null)

        assertEquals(latest, GetLatestVehicleTelemetryUseCase(repository)("v1").getOrThrow())
        assertNull(GetLatestVehicleTelemetryUseCase(repository)("v2").getOrThrow())

        val from = Instant.parse("2026-10-24T00:00:00Z")
        val to = Instant.parse("2026-10-24T12:00:00Z")
        val buckets = listOf(TelemetryHistoryBucket(from, "v1", 40, 88.0, 1800, 60.0, 13.8, 30))
        coEvery { repository.getVehicleTelemetryHistory("v1", from, to, "1 hour") } returns Result.success(buckets)

        val history = GetVehicleTelemetryHistoryUseCase(repository)("v1", from, to, "1 hour").getOrThrow()
        assertEquals(buckets, history)
        coVerify(exactly = 1) { repository.getVehicleTelemetryHistory("v1", from, to, "1 hour") }
    }

    @Test
    fun `ConvertAlertToAppointmentUseCase convierte la alerta en cita`() = runTest {
        val expected = ConvertedAppointment("apt-1", "v1", "c1", Instant.parse("2026-10-25T14:00:00Z"), "PENDING", "Chequeo")
        coEvery { repository.convertAlertToAppointment("a1") } returns Result.success(expected)

        val result = ConvertAlertToAppointmentUseCase(repository)("a1").getOrThrow()
        assertEquals("apt-1", result.appointmentId)
    }

    @Test
    fun `ObdResponseParser decodifica carga del motor 0104 y acelerador 0111`() {
        assertEquals(100.0, ObdResponseParser.engineLoad("4104FF")!!, 0.01)
        assertEquals(50.19, ObdResponseParser.throttlePosition("411180")!!, 0.1)
        assertNull(ObdResponseParser.engineLoad("NO DATA"))
        assertNull(ObdResponseParser.throttlePosition("NO DATA"))
    }
}
