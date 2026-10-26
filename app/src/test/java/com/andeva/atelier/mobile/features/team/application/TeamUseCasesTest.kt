package com.andeva.atelier.mobile.features.team.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.team.domain.PendingJustifications
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TeamUseCasesTest {

    private val repository = mockk<TeamRepository>()
    private val sessionManager = mockk<SessionManager>()
    private val date = LocalDate.of(2026, 10, 24)

    private fun session(permissions: List<Permission>) = Session(
        userId = "u1", email = "a@b.c", fullName = "Ana", tenantId = "t1", tenantName = "Taller",
        membershipId = "m1", permissions = permissions.map { it.value }.toSet(), plan = null, branchId = "b1"
    )

    private fun item(id: String = "a1") = TeamAttendance(
        id, "m2", "b1", null, null, null, null, null, null, null, null, null, null, null
    )

    @Test
    fun `sin audit_all la asistencia de la sede y la bandeja devuelven lista vacia sin llamar`() = runTest {
        coEvery { sessionManager.getSession() } returns session(listOf(Permission.JUSTIFICATIONS_APPROVE))

        val daily = GetBranchDailyAttendanceUseCase(repository, sessionManager)(date)
        val inbox = GetPendingJustificationsUseCase(repository, sessionManager)()

        assertEquals(emptyList<TeamAttendance>(), daily.getOrThrow())
        assertEquals(PendingJustifications(emptyList(), emptyList()), inbox.getOrThrow())
        confirmVerified(repository)
    }

    @Test
    fun `sin sesion tampoco se llama al repositorio`() = runTest {
        coEvery { sessionManager.getSession() } returns null

        assertEquals(emptyList<TeamAttendance>(), GetBranchDailyAttendanceUseCase(repository, sessionManager)(date).getOrThrow())
        confirmVerified(repository)
    }

    @Test
    fun `con audit_all delega la asistencia de la sede y la bandeja`() = runTest {
        coEvery { sessionManager.getSession() } returns session(listOf(Permission.ATTENDANCE_AUDIT_ALL))
        val pending = PendingJustifications(listOf(item()), listOf(date))
        coEvery { repository.getBranchDaily(date) } returns Result.success(listOf(item()))
        coEvery { repository.getPendingJustifications(3) } returns Result.success(pending)

        assertEquals(listOf(item()), GetBranchDailyAttendanceUseCase(repository, sessionManager)(date).getOrThrow())
        assertEquals(pending, GetPendingJustificationsUseCase(repository, sessionManager)(3).getOrThrow())
    }

    @Test
    fun `la bandeja pide 7 dias por defecto`() = runTest {
        coEvery { sessionManager.getSession() } returns session(listOf(Permission.ATTENDANCE_AUDIT_ALL))
        coEvery { repository.getPendingJustifications(7) } returns Result.success(PendingJustifications(emptyList(), emptyList()))

        GetPendingJustificationsUseCase(repository, sessionManager)()

        coVerify { repository.getPendingJustifications(7) }
    }

    @Test
    fun `el registro del colaborador se delega sin filtrar permisos`() = runTest {
        coEvery { repository.getCollaboratorAttendance("m2", "a1") } returns Result.success(item())
        coEvery { repository.getCollaboratorAttendance("m2", "zz") } returns Result.success(null)

        val useCase = GetCollaboratorAttendanceUseCase(repository)

        assertEquals("a1", useCase("m2", "a1").getOrThrow()!!.id)
        assertNull(useCase("m2", "zz").getOrThrow())
    }

    @Test
    fun `aprobar y rechazar delegan sin filtrar permisos`() = runTest {
        coEvery { repository.approveJustification("a1") } returns Result.success(item("a1"))
        coEvery { repository.rejectJustification("a2") } returns Result.failure(IllegalStateException("403"))

        assertEquals("a1", ApproveJustificationUseCase(repository)("a1").getOrThrow().id)
        assertEquals("403", RejectJustificationUseCase(repository)("a2").exceptionOrNull()!!.message)
    }
}
