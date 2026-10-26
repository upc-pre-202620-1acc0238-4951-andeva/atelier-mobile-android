package com.andeva.atelier.mobile.features.team.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.team.infrastructure.TeamTestEnv
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TeamRepositoryImplTest {

    private val env = TeamTestEnv()
    private lateinit var repository: TeamRepositoryImpl
    private val daily = "/api/v1/hr/attendances/branch/b1/daily"
    private val employees = "/api/v1/hr/employees/branch/b1"

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `sin sede falla con MissingBranchException sin llamar`() = runTest {
        env.signIn(branchId = null)

        assertTrue(repository.getBranchDaily(LocalDate.of(2026, 10, 24)).exceptionOrNull() is MissingBranchException)
        assertTrue(repository.getPendingJustifications().exceptionOrNull() is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `la bandeja pide los ultimos 7 dias de Lima y deja solo los PENDING`() = runTest {
        env.signIn()
        env.responses["GET $employees"] = env.json(emptyList<Any>())
        env.handler = { request ->
            if (!request.path!!.startsWith(daily)) null
            else when (request.requestUrl!!.queryParameter("date")) {
                "2026-10-24" -> env.json(
                    listOf(env.attendance("a24", justificationStatus = "PENDING"), env.attendance("a24x", justificationStatus = "APPROVED"))
                )
                "2026-10-20" -> env.json(listOf(env.attendance("a20", justificationStatus = "PENDING")))
                else -> env.json(listOf(env.attendance("ok", justificationStatus = null)))
            }
        }

        val result = repository.getPendingJustifications().getOrThrow()

        val dates = env.requested("GET", daily).map { it.requestUrl!!.queryParameter("date") }
        assertEquals((18..24).map { "2026-10-$it" }, dates.sortedBy { it })
        assertEquals(listOf("a24", "a20"), result.items.map { it.id })
        assertTrue(result.failedDates.isEmpty())
    }

    @Test
    fun `un dia que falla queda en failedDates`() = runTest {
        env.signIn(permissions = emptyList())
        env.handler = { request ->
            when (request.requestUrl!!.queryParameter("date")) {
                "2026-10-22" -> env.error(500, "BOOM", "Falla")
                "2026-10-21" -> env.json(listOf(env.attendance("a21", justificationStatus = "PENDING")))
                else -> env.json(emptyList<Any>())
            }
        }

        val result = repository.getPendingJustifications().getOrThrow()

        assertEquals(listOf(LocalDate.of(2026, 10, 22)), result.failedDates)
        assertEquals(listOf("a21"), result.items.map { it.id })
    }

    @Test
    fun `un item mal formado en un dia cuenta como dia fallido`() = runTest {
        env.signIn(permissions = emptyList())
        env.handler = { request ->
            when (request.requestUrl!!.queryParameter("date")) {
                "2026-10-23" -> env.json(listOf(env.attendance("a23", membershipId = null)))
                else -> env.json(emptyList<Any>())
            }
        }

        val result = repository.getPendingJustifications().getOrThrow()

        assertEquals(listOf(LocalDate.of(2026, 10, 23)), result.failedDates)
    }

    @Test
    fun `si fallan todos los dias la bandeja falla con el message del primero`() = runTest {
        env.signIn(permissions = emptyList())
        env.handler = { request ->
            env.error(500, "BOOM", "Falla ${request.requestUrl!!.queryParameter("date")}")
        }

        val error = repository.getPendingJustifications().exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals("Falla 2026-10-24", error!!.message)
    }

    @Test
    fun `los items se ordenan por dia reciente y luego por entrada descendente con nulos al final`() = runTest {
        env.signIn(permissions = emptyList())
        env.handler = { request ->
            when (request.requestUrl!!.queryParameter("date")) {
                "2026-10-24" -> env.json(
                    listOf(
                        env.attendance("sin", clockIn = null),
                        env.attendance("temprano", clockIn = "2026-10-24T12:00:00Z"),
                        env.attendance("tarde", clockIn = "2026-10-24T13:00:00Z")
                    )
                )
                "2026-10-23" -> env.json(listOf(env.attendance("ayer", clockIn = "2026-10-23T13:00:00Z")))
                else -> env.json(emptyList<Any>())
            }
        }

        val result = repository.getPendingJustifications().getOrThrow()

        assertEquals(listOf("tarde", "temprano", "sin", "ayer"), result.items.map { it.id })
    }

    @Test
    fun `una falta ABSENT sin clockIn se lee sin error`() = runTest {
        env.signIn()
        env.responses["GET $employees"] = env.json(listOf(env.profile("m1", "Mecánico")))
        env.responses["GET $daily"] = env.json(listOf(env.attendance("a1", clockIn = null, status = "ABSENT")))

        val item = repository.getBranchDaily(LocalDate.of(2026, 10, 24)).getOrThrow().single()

        assertNull(item.clockIn)
        assertEquals(AttendanceStatus.ABSENT, item.status)
        assertEquals("Mecánico", item.jobTitle)
        assertEquals("2026-10-24", env.requested("GET", daily).single().requestUrl!!.queryParameter("date"))
    }

    @Test
    fun `un item mal formado hace fallar la lectura del dia`() = runTest {
        env.signIn(permissions = emptyList())
        env.responses["GET $daily"] = env.json(listOf(env.attendance("a1"), env.attendance("a2", membershipId = null)))

        assertTrue(repository.getBranchDaily(LocalDate.of(2026, 10, 24)).exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `si falla el perfil el jobTitle queda null y la lista se devuelve`() = runTest {
        env.signIn()
        env.responses["GET $employees"] = env.error(403, "FORBIDDEN", "Sin acceso")
        env.responses["GET $daily"] = env.json(listOf(env.attendance("a1")))

        val item = repository.getBranchDaily(LocalDate.of(2026, 10, 24)).getOrThrow().single()

        assertNull(item.jobTitle)
    }

    @Test
    fun `sin members read no pide los perfiles`() = runTest {
        env.signIn(permissions = listOf(Permission.ATTENDANCE_AUDIT_ALL))
        env.responses["GET $daily"] = env.json(listOf(env.attendance("a1")))

        val item = repository.getBranchDaily(LocalDate.of(2026, 10, 24)).getOrThrow().single()

        assertNull(item.jobTitle)
        assertTrue(env.requested("GET", employees).isEmpty())
    }

    @Test
    fun `aprobar devuelve EXCUSED`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/a1/justification/approve"] =
            env.json(env.attendance("a1", status = "EXCUSED", justificationStatus = "APPROVED"))

        val result = repository.approveJustification("a1").getOrThrow()

        assertEquals(AttendanceStatus.EXCUSED, result.status)
        assertEquals(JustificationStatus.APPROVED, result.justificationStatus)
    }

    @Test
    fun `rechazar devuelve el descargo rechazado`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/a1/justification/reject"] =
            env.json(env.attendance("a1", status = "ABSENT", justificationStatus = "REJECTED"))

        assertEquals(JustificationStatus.REJECTED, repository.rejectJustification("a1").getOrThrow().justificationStatus)
    }

    @Test
    fun `descargo ya revisado devuelve el message del backend`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/a1/justification/approve"] =
            env.error(409, "ALREADY_REVIEWED", "El descargo ya fue revisado")

        val error = repository.approveJustification("a1").exceptionOrNull() as ApiException

        assertEquals(409, error.httpStatus)
        assertEquals("El descargo ya fue revisado", error.message)
    }

    @Test
    fun `aprobar con respuesta no mapeable falla`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/hr/attendances/a1/justification/approve"] = env.json(env.attendance("a1", membershipId = null))

        assertTrue(repository.approveJustification("a1").isFailure)
    }

    @Test
    fun `el historial pide los ultimos 30 dias de Lima y busca por attendanceId`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/hr/attendances/employee/m1/history"] =
            env.json(listOf(env.attendance("a1"), env.attendance("a2", clockIn = "2026-10-20T13:00:00Z", status = "ON_TIME")))

        val found = repository.getCollaboratorAttendance("m1", "a2").getOrThrow()
        val missing = repository.getCollaboratorAttendance("m1", "nope").getOrThrow()

        assertEquals("a2", found!!.id)
        assertNull(missing)
        val url = env.requests.first().requestUrl!!
        assertEquals("2026-09-25", url.queryParameter("startDate"))
        assertEquals("2026-10-24", url.queryParameter("endDate"))
    }

    @Test
    fun `un dia con cuerpo ilegible queda en failedDates y los demas se devuelven`() = runTest {
        env.signIn(permissions = emptyList())
        env.handler = { request ->
            when (request.requestUrl!!.queryParameter("date")) {
                "2026-10-22" -> env.json(mapOf("not" to "an array"))
                "2026-10-21" -> env.json(listOf(env.attendance("a21", justificationStatus = "PENDING")))
                "2026-10-20" -> env.json(listOf(mapOf("id" to "x", "branchId" to "b1", "membershipId" to "m1", "workedMinutes" to "abc")))
                else -> env.json(emptyList<Any>())
            }
        }

        val result = repository.getPendingJustifications().getOrThrow()

        assertEquals(listOf(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 20)), result.failedDates)
        assertEquals(listOf("a21"), result.items.map { it.id })
    }

    @Test
    fun `un cuerpo de perfiles ilegible deja jobTitle null y devuelve las asistencias`() = runTest {
        env.signIn()
        env.responses["GET $employees"] = env.json(mapOf("not" to "an array"))
        env.responses["GET $daily"] = env.json(listOf(env.attendance("a1")))

        val item = repository.getBranchDaily(LocalDate.of(2026, 10, 24)).getOrThrow().single()

        assertNull(item.jobTitle)
    }

    @Test
    fun `a las 22h de Lima el dia de negocio sigue siendo el de Lima`() = runTest {
        val late = TeamTestEnv(java.time.Instant.parse("2026-10-25T03:00:00Z"))
        late.start()
        try {
            late.signIn(permissions = emptyList())
            late.handler = { late.json(emptyList<Any>()) }

            late.repository().getPendingJustifications().getOrThrow()

            val dates = late.requested("GET", daily).map { it.requestUrl!!.queryParameter("date") }
            assertEquals((18..24).map { "2026-10-$it" }, dates.sortedBy { it })
        } finally {
            late.stop()
        }
    }
}
