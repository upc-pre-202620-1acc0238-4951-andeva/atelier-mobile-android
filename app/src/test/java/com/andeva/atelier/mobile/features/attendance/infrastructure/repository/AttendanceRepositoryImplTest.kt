package com.andeva.atelier.mobile.features.attendance.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.MissingAssignmentException
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.infrastructure.AttendanceTestEnv
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.ClockInHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.ClockOutHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.JustificationHandler
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AttendanceRepositoryImplTest {

    private val env = AttendanceTestEnv()
    private lateinit var repository: AttendanceRepositoryImpl
    private val location = DeviceLocation(-12.0464, -77.0428, 10f, isMock = false)

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    private suspend fun pending() = env.db.pendingOperationDao().observeForUser("u1").first()

    @Test
    fun `ingreso con red se registra y queda en Room sin hora del dispositivo`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.json(env.attendance(), 201)

        val outcome = repository.clockIn("b1", "s1", location).getOrThrow()

        assertTrue(outcome is SubmitOutcome.Recorded)
        val body = env.requested("POST", "/api/v1/hr/attendances/clock-in").single().body.readUtf8()
        assertTrue(body.contains("\"branchId\":\"b1\""))
        assertFalse(body.contains("clockInTime"))
        assertTrue(repository.observeToday().first() is TodayAttendance.ClockedIn)
        assertTrue(pending().isEmpty())
    }

    @Test
    fun `error de negocio con red vuelve con el message del backend y no se encola`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/clock-in"] =
            env.error(422, "UNPROCESSABLE_ENTITY", "Estás a 350 m de la sede")

        val error = repository.clockIn("b1", "s1", location).exceptionOrNull()

        assertEquals(422, (error as ApiException).httpStatus)
        assertEquals("Estás a 350 m de la sede", error.message)
        assertTrue(pending().isEmpty())
    }

    @Test
    fun `sin red el ingreso se encola con la hora y la dedupe_key del dia de Lima`() = runTest {
        env.signIn()
        env.isOnline.value = false

        assertEquals(SubmitOutcome.Queued, repository.clockIn("b1", "s1", location).getOrThrow())
        assertEquals(SubmitOutcome.AlreadyQueued, repository.clockIn("b1", "s1", location).getOrThrow())

        val operation = pending().single()
        assertEquals(ClockInHandler.TYPE, operation.type)
        assertEquals("CLOCK_IN:m1:2026-10-24", operation.dedupeKey)
        assertTrue(operation.payload.contains("\"clockInTime\":\"2026-10-24T14:00:00Z\""))
        assertTrue(env.requests.isEmpty())
        assertTrue(repository.observeToday().first() is TodayAttendance.ClockInQueued)
        verify { env.syncScheduler.requestSync() }
    }

    @Test
    fun `si la llamada se corta se encola`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.disconnect()

        assertEquals(SubmitOutcome.Queued, repository.clockIn("b1", "s1", location).getOrThrow())
        assertEquals(ClockInHandler.TYPE, pending().single().type)
    }

    @Test
    fun `la salida va a la cola detras del ingreso pendiente aunque haya red`() = runTest {
        env.signIn()
        env.isOnline.value = false
        repository.clockIn("b1", "s1", location)
        env.isOnline.value = true

        assertEquals(SubmitOutcome.Queued, repository.clockOut().getOrThrow())

        val (clockIn, clockOut) = pending().sortedBy { it.type }
        assertEquals(ClockOutHandler.TYPE, clockOut.type)
        assertEquals(clockIn.id, clockOut.dependsOn)
        assertEquals("CLOCK_OUT:m1:2026-10-24", clockOut.dedupeKey)
        assertTrue(clockOut.payload.contains("\"clockOutTime\":\"2026-10-24T14:00:00Z\""))
        assertTrue(env.requests.isEmpty())
        assertTrue(repository.observeToday().first() is TodayAttendance.ClockOutQueued)
    }

    @Test
    fun `salida con red cierra la jornada abierta de la cache`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/clock-in"] = env.json(env.attendance(), 201)
        repository.clockIn("b1", "s1", location)
        env.responses["POST /api/v1/hr/attendances/clock-out"] =
            env.json(env.attendance(clockOut = "2026-10-24T22:05:00Z"))

        val outcome = repository.clockOut().getOrThrow() as SubmitOutcome.Recorded

        assertEquals(480L, outcome.attendance.workedMinutes)
        assertTrue(env.requested("POST", "/api/v1/hr/attendances/clock-out").single().body.readUtf8().contains("\"attendanceId\":\"a1\""))
        assertTrue(repository.observeToday().first() is TodayAttendance.Completed)
    }

    @Test
    fun `descargo sin red se encola con el id de la asistencia`() = runTest {
        env.signIn()
        env.isOnline.value = false

        assertEquals(SubmitOutcome.Queued, repository.submitJustification(AttendanceId("a1"), "  Tráfico  ").getOrThrow())

        val operation = pending().single()
        assertEquals(JustificationHandler.TYPE, operation.type)
        assertEquals("JUSTIFICATION:a1", operation.dedupeKey)
        assertTrue(operation.payload.contains("\"attendanceId\":\"a1\""))
        assertTrue(operation.payload.contains("\"reason\":\"Tráfico\""))
    }

    @Test
    fun `sin membershipId no se puede marcar`() = runTest {
        assertTrue(repository.clockIn("b1", "s1", location).exceptionOrNull() is MissingAssignmentException)
    }

    @Test
    fun `refresh actualiza sede y turno, sedes, turno e historial de 30 dias`() = runTest {
        env.signIn(branchId = null, shiftId = null)
        env.responses["GET /api/v1/hr/employees/membership/m1"] =
            env.json(mapOf("id" to "p1", "branchId" to "b1", "membershipId" to "m1", "assignedShiftId" to "s1"))
        env.responses["GET /api/v1/branches"] = env.json(
            listOf(mapOf("id" to "b1", "name" to "Sede Lima", "latitude" to -12.0464, "longitude" to -77.0428, "geofenceRadiusMeters" to 100, "isActive" to true))
        )
        env.responses["GET /api/v1/hr/work-shifts/s1"] = env.json(
            mapOf(
                "id" to "s1", "name" to "Mañana", "startTime" to "08:00:00", "endTime" to "17:00:00", "gracePeriodMinutes" to 15,
                "isActive" to true, "workDays" to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"),
                "restDays" to listOf("SUNDAY")
            )
        )
        env.responses["GET /api/v1/hr/attendances/employee/m1/history"] = env.json(
            listOf(env.attendance(id = "old", clockIn = "2026-10-20T13:00:00Z", clockOut = "2026-10-20T22:00:00Z", status = "LATE"))
        )
        // status-today sin jornada: 404 (no documentado), se ignora

        assertTrue(repository.refresh().isSuccess)

        val history = env.requested("GET", "/api/v1/hr/attendances/employee/m1/history").single()
        assertEquals("2026-09-24", history.requestUrl!!.queryParameter("startDate"))
        assertEquals("2026-10-24", history.requestUrl!!.queryParameter("endDate"))
        assertEquals("s1", env.sessionManager.getSession()!!.shiftId)

        val schedule = repository.observeSchedule().first()!!
        assertEquals(15, schedule.shift.gracePeriodMinutes)
        assertEquals("Sede Lima", schedule.branch!!.name)
        assertEquals(listOf(DayOfWeek.SUNDAY), schedule.days.filterNot { it.isWorkDay }.map { it.dayOfWeek })
        assertNotNull(repository.findBranch("b1"))
        assertEquals(listOf("old"), repository.observeHistory().first().map { it.id.value })
        assertTrue(repository.observeToday().first() is TodayAttendance.NotClockedIn)

        // Sedes y turno valen 24 h: un segundo refresh solo vuelve a pedir perfil y asistencia
        repository.refresh()
        assertEquals(1, env.requested("GET", "/api/v1/branches").size)
        assertEquals(1, env.requested("GET", "/api/v1/hr/work-shifts/s1").size)
        assertEquals(2, env.requested("GET", "/api/v1/hr/attendances/employee/m1/history").size)
    }

    @Test
    fun `refresh sin perfil laboral borra la sede y no pide lo que la sesion no permite`() = runTest {
        env.signIn(permissions = emptyList())
        assertTrue(repository.refresh().isSuccess)
        assertTrue(env.requests.isEmpty())

        env.signIn(permissions = listOf(com.andeva.atelier.mobile.core.domain.Permission.ATTENDANCE_READ_OWN))
        env.responses["GET /api/v1/hr/attendances/employee/m1/history"] = env.json(emptyList<Any>())
        repository.refresh()
        assertNull(env.sessionManager.getSession()!!.shiftId)
        assertNull(repository.observeSchedule().first())
    }
}
