package com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.FaultSeverity
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.DiagnosticsTestEnv
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.RegisterFaultRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.ResolveFaultPayload
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.TelemetryBatchRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.RegisterFaultHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.ResolveFaultHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.TelemetryBatchHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiagnosticsRepositoryImplTest {

    private val env = DiagnosticsTestEnv()
    private lateinit var repository: DiagnosticsRepositoryImpl

    @Before
    fun setUp() = runTest {
        env.start()
        env.signIn()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    private fun dtc(code: String) = Dtc.of(code)!!
    private suspend fun queued() = env.db.pendingOperationDao().observeForUser("u1").first()

    private fun sample(second: Long, temp: Double? = 88.0) =
        TelemetrySample(Instant.parse("2026-10-24T13:00:00Z").plusSeconds(second), temp, 820, 0, 13.9, 61.0, 35)

    @Test
    fun `instalacion activa, sin instalacion y dispositivos`() = runTest {
        assertNull(repository.getActiveInstallation("v1").getOrThrow())

        env.responses["GET /api/v1/iot/installations/vehicle/v1/active"] = env.json(env.installation())
        assertEquals("i1", repository.getActiveInstallation("v1").getOrThrow()?.id)

        env.responses["GET /api/v1/iot/devices"] = env.json(
            listOf(mapOf("id" to "d1", "deviceIdentifier" to "AA:BB:CC:11:22:33", "connectionType" to "BLUETOOTH_BLE", "status" to "ACTIVE"))
        )
        val device = repository.getDevices(DeviceStatus.ACTIVE).getOrThrow().single()
        assertTrue(device.matches("aa:bb:cc:11:22:33"))
        assertEquals("ACTIVE", env.requested("GET", "/api/v1/iot/devices").single().requestUrl!!.queryParameter("status"))
    }

    @Test
    fun `instalar envia el odometro y el 409 llega con el message`() = runTest {
        env.responses["POST /api/v1/iot/installations/install"] = env.json(env.installation(), 201)
        assertTrue(repository.installDevice("d1", "v1", 45000).getOrThrow().isActive)
        assertTrue(env.requested("POST", "/api/v1/iot/installations/install").single().body.readUtf8().contains("\"currentOdometerKm\":45000"))

        env.responses["POST /api/v1/iot/installations/install"] = env.error(409, "ERR_ACTIVE_INSTALLATION_CONFLICT", "El vehículo ya tiene un escáner")
        val error = repository.installDevice("d1", "v1", null).exceptionOrNull() as ApiException
        assertEquals("El vehículo ya tiene un escáner", error.message)
    }

    @Test
    fun `solo se guardan las muestras con temperatura y sin repetir el instante`() = runTest {
        assertTrue(repository.saveReading("v1", sample(0), -12.04, -77.04))
        assertFalse(repository.saveReading("v1", sample(0), null, null))
        assertFalse(repository.saveReading("v1", sample(2, temp = null), null, null))

        val row = env.db.telemetryReadingDao().findForVehicle("v1").single()
        assertEquals(-12.04, row.latitude!!, 0.0)
    }

    @Test
    fun `las lecturas salen en lotes de 200 sin repetirse`() = runTest {
        repeat(450) { repository.saveReading("v1", sample(it * 2L), null, null) }

        assertEquals(2, repository.enqueueUnbatchedReadings(onlyFull = true))
        assertEquals(0, repository.enqueueUnbatchedReadings(onlyFull = true))
        assertEquals(1, repository.enqueueUnbatchedReadings(onlyFull = false))
        assertEquals(0, repository.enqueueUnbatchedReadings(onlyFull = false))

        val batches = queued().filter { it.type == TelemetryBatchHandler.TYPE }
        assertEquals(listOf(200, 200, 50), batches.map { env.gson.fromJson(it.payload, TelemetryBatchRequestDto::class.java).readings.size })
        val first = env.gson.fromJson(batches.first().payload, TelemetryBatchRequestDto::class.java).readings.first()
        assertEquals("2026-10-24T13:00:00Z", first.timestamp)
        assertNull(first.latitude)
        assertFalse(env.gson.toJson(first).contains("latitude"))       // los opcionales sin dato no se envían
        assertEquals(450, repository.observeSyncState("v1").first().pendingReadings)
    }

    @Test
    fun `las lecturas confirmadas se purgan a los 7 dias`() = runTest {
        repository.saveReading("v1", TelemetrySample(Instant.parse("2026-10-10T00:00:00Z"), 80.0, null, null, null, null, null), null, null)
        repository.saveReading("v1", sample(0), null, null)
        repository.enqueueUnbatchedReadings(onlyFull = false)

        repository.purgeConfirmedReadings()
        assertEquals(2, env.db.telemetryReadingDao().findForVehicle("v1").size)     // el lote sigue en cola

        env.db.pendingOperationDao().deleteForUser("u1")                            // el backend lo confirmó
        repository.purgeConfirmedReadings()
        assertEquals(listOf(sample(0).timestamp.toEpochMilli()), env.db.telemetryReadingDao().findForVehicle("v1").map { it.timestamp })
        assertEquals(0, repository.observeSyncState("v1").first().pendingReadings)
    }

    @Test
    fun `los codigos nuevos quedan locales con su REGISTER_FAULT y los activos no se repiten`() = runTest {
        env.db.vehicleFaultDao().upsert(VehicleFaultEntity("f1", "v1", "P0133", "LOW", "O2", 0, false, null, 0))

        val faults = repository.registerDetectedFaults("v1", listOf(dtc("P0133"), dtc("P0304")), "s1")

        assertEquals("f1", faults[0].id)
        assertTrue(faults[1].isPending)
        assertEquals(FaultSeverity.CRITICAL, faults[1].severity)
        val operation = queued().single()
        assertEquals(RegisterFaultHandler.TYPE, operation.type)
        assertEquals("FAULT:v1:P0304:s1", operation.dedupeKey)
        assertEquals("local-${operation.id}", faults[1].id)
        val body = env.gson.fromJson(operation.payload, RegisterFaultRequestDto::class.java)
        assertEquals("CRITICAL", body.severity)
        assertEquals(1, repository.observeSyncState("v1").first().pendingFaults)

        // Volver a escanear en la misma sesión devuelve la misma falla
        assertEquals(faults[1].id, repository.registerDetectedFaults("v1", listOf(dtc("P0304")), "s1").single().id)
        assertEquals(1, queued().size)
    }

    @Test
    fun `al borrar se resuelven las reales, se descartan las locales y se reportan las persistentes`() = runTest {
        env.db.vehicleFaultDao().upsert(VehicleFaultEntity("f1", "v1", "P0133", "LOW", "O2", 0, false, null, 0))
        env.db.vehicleFaultDao().upsert(VehicleFaultEntity("f2", "v1", "P0420", "MEDIUM", "Cat", 0, false, null, 0))
        val local = repository.registerDetectedFaults("v1", listOf(dtc("P0304")), "s1").single()

        val result = repository.applyClearedFaults("v1", remaining = listOf(dtc("P0420")), sessionId = "s1")

        assertEquals(setOf("f1", local.id), result.cleared.map { it.id }.toSet())
        assertEquals(listOf("f2"), result.persistent.map { it.id })
        assertFalse(result.isClean)
        val operations = queued()
        assertEquals(listOf(ResolveFaultHandler.TYPE), operations.map { it.type })
        assertEquals("RESOLVE_FAULT:f1", operations.single().dedupeKey)
        assertNull(env.db.vehicleFaultDao().findById(local.id))
        assertEquals(listOf("f2"), repository.observeActiveFaults("v1").first().map { it.id })
    }

    @Test
    fun `una falla local que se esta enviando se resuelve despues de su registro`() = runTest {
        val local = repository.registerDetectedFaults("v1", listOf(dtc("P0304")), "s1").single()
        val registerId = queued().single().id
        env.db.pendingOperationDao().markSyncing(registerId)

        repository.applyClearedFaults("v1", remaining = emptyList(), sessionId = "s1")

        val resolve = queued().single { it.type == ResolveFaultHandler.TYPE }
        assertEquals(registerId, resolve.dependsOn)
        assertEquals(local.id, env.gson.fromJson(resolve.payload, ResolveFaultPayload::class.java).faultId)
        assertTrue(env.db.vehicleFaultDao().findById(local.id)!!.isResolved)
    }

    @Test
    fun `recargar las fallas conserva las locales y las que se estan resolviendo`() = runTest {
        val local = repository.registerDetectedFaults("v1", listOf(dtc("P0304")), "s1").single()
        env.db.vehicleFaultDao().upsert(VehicleFaultEntity("f1", "v1", "P0133", "LOW", "O2", 0, false, null, 0))
        repository.applyClearedFaults("v1", remaining = listOf(dtc("P0304")), sessionId = "s1")      // f1 se resuelve en cola
        env.responses["GET /api/v1/iot/faults/vehicle/v1/active"] = env.json(listOf(env.fault("f1", "P0133"), env.fault("f3", "P0171")))

        repository.refreshActiveFaults("v1").getOrThrow()

        assertEquals(setOf(local.id, "f3"), repository.observeActiveFaults("v1").first().map { it.id }.toSet())
    }

    @Test
    fun `sin permiso no se piden fallas ni alertas`() = runTest {
        env.signIn(permissions = emptyList())
        assertTrue(repository.refreshActiveFaults("v1").isSuccess)
        assertTrue(repository.refreshPredictiveAlerts("v1").isSuccess)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `alertas predictivas en cache y marcar como revisada`() = runTest {
        env.responses["GET /api/v1/iot/alerts/vehicle/v1"] = env.json(listOf(env.alert("a1"), env.alert("a2")))
        repository.refreshPredictiveAlerts("v1").getOrThrow()
        assertEquals(2, repository.observePredictiveAlerts("v1").first().size)

        env.responses["PATCH /api/v1/iot/alerts/a1/acknowledge"] = env.json(env.alert("a1", status = "ACKNOWLEDGED"))
        val alert = repository.acknowledgePredictiveAlert("a1").getOrThrow()
        assertEquals("s1", alert.recommendedServiceId)
        assertEquals("ACKNOWLEDGED", repository.observePredictiveAlerts("v1").first().first { it.id == "a1" }.status?.name)
    }

    @Test
    fun `alertas del taller y convertir alerta en cita preventiva`() = runTest {
        env.responses["GET /api/v1/iot/alerts/tenant"] = env.json(listOf(env.alert("a1"), env.alert("a2")))
        val alerts = repository.getTenantPredictiveAlerts().getOrThrow()
        assertEquals(2, alerts.size)
        assertEquals(2, repository.observeAllPredictiveAlerts().first().size)

        env.responses["POST /api/v1/iot/alerts/a1/convert-to-appointment"] = env.json(
            mapOf(
                "appointmentId" to "apt-1",
                "vehicleId" to "v1",
                "customerId" to "c1",
                "scheduledAt" to "2026-10-25T15:00:00Z",
                "status" to "PENDING",
                "notes" to "Revisar sistema de refrigeración"
            )
        )
        val appointment = repository.convertAlertToAppointment("a1").getOrThrow()
        assertEquals("apt-1", appointment.appointmentId)
        assertEquals("v1", appointment.vehicleId)
        assertEquals(Instant.parse("2026-10-25T15:00:00Z"), appointment.scheduledAt)
        assertEquals("Revisar sistema de refrigeración", appointment.notes)
    }

    @Test
    fun `telemetria remota latest e historial por bucket`() = runTest {
        assertNull(repository.getLatestVehicleTelemetry("v1").getOrThrow())

        env.responses["GET /api/v1/iot/telemetry/vehicle/v1/latest"] = env.json(
            mapOf(
                "vehicleId" to "v1",
                "timestamp" to "2026-10-24T13:55:00Z",
                "speedKmh" to 42,
                "engineTempCelsius" to 92.5,
                "engineRpm" to 2100,
                "batteryVoltage" to 13.8,
                "fuelPercentage" to 64.0
            )
        )
        val latest = repository.getLatestVehicleTelemetry("v1").getOrThrow()!!
        assertEquals(42, latest.speedKmh)
        assertEquals(92.5, latest.engineTempCelsius!!, 0.0)
        assertEquals(2100, latest.engineRpm)

        env.responses["GET /api/v1/iot/telemetry/vehicle/v1/history"] = env.json(
            listOf(
                mapOf(
                    "bucketTime" to "2026-10-24T13:00:00Z",
                    "vehicleId" to "v1",
                    "avgSpeedKmh" to 38,
                    "avgEngineTempCelsius" to 90.0,
                    "avgEngineRpm" to 1950,
                    "avgFuelPercentage" to 65.0,
                    "avgBatteryVoltage" to 13.9,
                    "sampleCount" to 120
                )
            )
        )
        val from = Instant.parse("2026-10-24T12:00:00Z")
        val to = Instant.parse("2026-10-24T14:00:00Z")
        val history = repository.getVehicleTelemetryHistory("v1", from, to, "1 hour").getOrThrow()
        assertEquals(1, history.size)
        assertEquals(120, history.single().sampleCount)
        val url = env.requested("GET", "/api/v1/iot/telemetry/vehicle/v1/history").single().requestUrl!!
        assertEquals("2026-10-24T12:00:00Z", url.queryParameter("from"))
        assertEquals("2026-10-24T14:00:00Z", url.queryParameter("to"))
        assertEquals("1 hour", url.queryParameter("bucket"))
    }
}
