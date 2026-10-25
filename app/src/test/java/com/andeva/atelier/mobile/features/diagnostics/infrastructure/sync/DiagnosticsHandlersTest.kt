package com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync

import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.DiagnosticsTestEnv
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiagnosticsHandlersTest {

    private val env = DiagnosticsTestEnv()
    private lateinit var batch: TelemetryBatchHandler
    private lateinit var register: RegisterFaultHandler
    private lateinit var resolve: ResolveFaultHandler

    @Before
    fun setUp() = runTest {
        env.start()
        env.signIn()
        batch = TelemetryBatchHandler(env.iotService, env.gson)
        register = RegisterFaultHandler(env.iotService, env.db.vehicleFaultDao(), env.gson, env.clock)
        resolve = ResolveFaultHandler(env.iotService, env.db.vehicleFaultDao(), env.gson, env.clock)
    }

    @After
    fun tearDown() = env.stop()

    private fun op(id: String, type: String, payload: String) = PendingOperationEntity(
        id = id, type = type, payload = payload, dedupeKey = "$type:$id", userId = "u1", tenantId = "t1",
        nextAttemptAt = 0, createdAt = 0
    )

    private val batchOp = op(
        "b1", TelemetryBatchHandler.TYPE,
        """{"vehicleId":"v1","readings":[{"timestamp":"2026-10-24T13:00:00Z","engineTempCelsius":88.0,"engineRpm":820}]}"""
    )
    private val registerOp = op(
        "op1", RegisterFaultHandler.TYPE,
        """{"vehicleId":"v1","dtcCode":"P0304","severity":"CRITICAL","description":"Falla de encendido en el cilindro 4"}"""
    )

    private fun local(resolved: Boolean = false) =
        VehicleFaultEntity("local-op1", "v1", "P0304", "CRITICAL", "Cilindro 4", 0, resolved, if (resolved) 5 else null, null)

    @Test
    fun `TELEMETRY_BATCH 202 se aplica y 404 sin instalacion falla con el message`() = runTest {
        env.responses["POST /api/v1/iot/telemetry/batch"] = env.json(mapOf("vehicleId" to "v1", "ingestedCount" to 1), 202)
        assertEquals(OperationResult.Applied, batch.send(batchOp))
        assertTrue(env.requested("POST", "/api/v1/iot/telemetry/batch").single().body.readUtf8().contains("\"engineTempCelsius\":88.0"))

        env.responses["POST /api/v1/iot/telemetry/batch"] = env.error(404, "NOT_FOUND", "El vehículo no tiene un escáner instalado")
        assertEquals("El vehículo no tiene un escáner instalado", (batch.send(batchOp) as OperationResult.Failed).message)

        env.responses["POST /api/v1/iot/telemetry/batch"] = env.disconnect()
        assertTrue(batch.send(batchOp) is OperationResult.Retry)
    }

    @Test
    fun `REGISTER_FAULT reemplaza la falla local por la del backend`() = runTest {
        env.db.vehicleFaultDao().upsert(local())
        env.responses["POST /api/v1/iot/faults"] = env.json(env.fault("f9", "P0304"), 201)

        assertEquals(OperationResult.Applied, register.send(registerOp))

        assertNull(env.db.vehicleFaultDao().findById("local-op1"))
        assertEquals("P0304", env.db.vehicleFaultDao().findById("f9")!!.dtcCode)
    }

    @Test
    fun `REGISTER_FAULT conserva la marca de borrada para su RESOLVE_FAULT`() = runTest {
        env.db.vehicleFaultDao().upsert(local(resolved = true))
        env.responses["POST /api/v1/iot/faults"] = env.json(env.fault("f9", "P0304"), 201)

        register.send(registerOp)

        assertTrue(env.db.vehicleFaultDao().findById("f9")!!.isResolved)
    }

    @Test
    fun `RESOLVE_FAULT con id real o local`() = runTest {
        env.responses["POST /api/v1/iot/faults/f9/resolve"] = env.json(env.fault("f9", "P0304", resolved = true))
        val real = op("r1", ResolveFaultHandler.TYPE, """{"faultId":"f9","vehicleId":"v1","dtcCode":"P0304"}""")
        assertEquals(OperationResult.Applied, resolve.send(real))
        assertTrue(env.db.vehicleFaultDao().findById("f9")!!.isResolved)

        // Local: el id real se busca entre las activas del vehículo
        env.requests.clear()
        env.responses["GET /api/v1/iot/faults/vehicle/v1/active"] = env.json(listOf(env.fault("f9", "P0304")))
        val pending = op("r2", ResolveFaultHandler.TYPE, """{"faultId":"local-op1","vehicleId":"v1","dtcCode":"P0304"}""")
        assertEquals(OperationResult.Applied, resolve.send(pending))
        assertEquals(1, env.requested("POST", "/api/v1/iot/faults/f9/resolve").size)

        // Si ya no está activa no hay nada que resolver
        env.requests.clear()
        env.responses["GET /api/v1/iot/faults/vehicle/v1/active"] = env.json(emptyList<Any>())
        assertEquals(OperationResult.Applied, resolve.send(pending))
        assertTrue(env.requested("POST", "/api/v1/iot/faults/f9/resolve").isEmpty())
    }
}
