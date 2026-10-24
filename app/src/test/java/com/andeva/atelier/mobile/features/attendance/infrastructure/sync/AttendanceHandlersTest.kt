package com.andeva.atelier.mobile.features.attendance.infrastructure.sync

import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.AttendanceTestEnv
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AttendanceHandlersTest {

    private val env = AttendanceTestEnv()
    private lateinit var clockIn: ClockInHandler
    private lateinit var clockOut: ClockOutHandler
    private lateinit var justification: JustificationHandler

    private val history = "GET /api/v1/hr/attendances/employee/m1/history"

    @Before
    fun setUp() = runTest {
        env.start()
        env.signIn()
        clockIn = ClockInHandler(env.attendanceService, env.cache, env.sessionManager, env.gson, env.clock)
        clockOut = ClockOutHandler(env.attendanceService, env.cache, env.sessionManager, env.gson, env.clock)
        justification = JustificationHandler(env.attendanceService, env.cache, env.db.attendanceDao(), env.sessionManager, env.gson)
    }

    @After
    fun tearDown() = env.stop()

    private fun op(type: String, payload: String) = PendingOperationEntity(
        id = "op1", type = type, payload = payload, dedupeKey = "$type:1", userId = "u1", tenantId = "t1",
        nextAttemptAt = 0, createdAt = 0
    )

    private val clockInOp = op(
        ClockInHandler.TYPE,
        """{"branchId":"b1","shiftId":"s1","latitude":-12.0464,"longitude":-77.0428,"clockInTime":"2026-10-24T13:05:00Z"}"""
    )
    private val clockOutOp = op(ClockOutHandler.TYPE, """{"attendanceId":"a1","clockOutTime":"2026-10-24T22:05:00Z"}""")

    @Test
    fun `CLOCK_IN 201 se aplica y guarda la asistencia`() = runTest {
        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.json(env.attendance(), 201)

        assertEquals(OperationResult.Applied, clockIn.send(clockInOp))
        assertTrue(env.requested("POST", "/api/v1/hr/attendances/clock-in").single().body.readUtf8().contains("clockInTime"))
        assertNotNull(env.db.attendanceDao().findById("a1"))
    }

    @Test
    fun `CLOCK_IN 409 cuenta como aplicada y recarga el dia de la captura`() = runTest {
        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.error(409, "CONFLICT", "Ya tienes una jornada abierta")
        env.responses[history] = env.json(listOf(env.attendance()))

        assertEquals(OperationResult.Applied, clockIn.send(clockInOp))
        val request = env.requested("GET", "/api/v1/hr/attendances/employee/m1/history").single()
        assertEquals("2026-10-24", request.requestUrl!!.queryParameter("startDate"))
        assertNotNull(env.db.attendanceDao().findById("a1"))
    }

    @Test
    fun `CLOCK_IN 422 falla con el message y 500 se reintenta`() = runTest {
        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.error(422, "UNPROCESSABLE_ENTITY", "Estás a 350 m de la sede")
        val failed = clockIn.send(clockInOp) as OperationResult.Failed
        assertEquals("Estás a 350 m de la sede", failed.message)

        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.error(500, "INTERNAL_ERROR", "Error")
        assertTrue(clockIn.send(clockInOp) is OperationResult.Retry)
    }

    @Test
    fun `CLOCK_OUT 400 o 404 con la salida ya en history cuenta como aplicada`() = runTest {
        env.responses[history] = env.json(listOf(env.attendance(clockOut = "2026-10-24T22:05:00Z")))

        env.responses["POST /api/v1/hr/attendances/clock-out"] = env.error(400, "BAD_REQUEST", "Salida ya registrada")
        assertEquals(OperationResult.Applied, clockOut.send(clockOutOp))

        env.responses["POST /api/v1/hr/attendances/clock-out"] = env.error(404, "NOT_FOUND", "No hay jornada abierta")
        assertEquals(OperationResult.Applied, clockOut.send(clockOutOp))
    }

    @Test
    fun `CLOCK_OUT 404 sin salida en history falla con el message del backend`() = runTest {
        env.responses[history] = env.json(emptyList<Any>())
        env.responses["POST /api/v1/hr/attendances/clock-out"] = env.error(404, "NOT_FOUND", "No hay jornada abierta")

        val failed = clockOut.send(clockOutOp) as OperationResult.Failed

        assertEquals(404, failed.httpStatus)
        assertEquals("No hay jornada abierta", failed.message)
    }

    @Test
    fun `CLOCK_OUT 404 sin poder verificar se reintenta`() = runTest {
        env.responses[history] = env.disconnect()
        env.responses["POST /api/v1/hr/attendances/clock-out"] = env.error(404, "NOT_FOUND", "No hay jornada abierta")

        assertTrue(clockOut.send(clockOutOp) is OperationResult.Retry)
    }

    @Test
    fun `CLOCK_OUT 200 guarda la jornada cerrada`() = runTest {
        env.responses["POST /api/v1/hr/attendances/clock-out"] = env.json(env.attendance(clockOut = "2026-10-24T22:05:00Z"))

        assertEquals(OperationResult.Applied, clockOut.send(clockOutOp))
        assertNotNull(env.db.attendanceDao().findById("a1")!!.clockOut)
    }

    @Test
    fun `SUBMIT_JUSTIFICATION 422 ya justificada cuenta como aplicada`() = runTest {
        val payload = """{"attendanceId":"a1","reason":"Tráfico"}"""
        env.responses["POST /api/v1/hr/attendances/a1/justification"] = env.error(422, "UNPROCESSABLE_ENTITY", "Ya justificada")

        assertEquals(OperationResult.Applied, justification.send(op(JustificationHandler.TYPE, payload)))
        // La asistencia no estaba en caché: no hay día que recargar
        assertTrue(env.requested("GET", "/api/v1/hr/attendances/employee/m1/history").isEmpty())
        assertNull(env.db.attendanceDao().findById("a1"))
        assertEquals(
            """{"reason":"Tráfico"}""",
            env.requested("POST", "/api/v1/hr/attendances/a1/justification").single().body.readUtf8()
        )
    }
}
