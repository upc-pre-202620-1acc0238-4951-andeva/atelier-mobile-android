package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayUnavailableException
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BayUseCasesTest {

    private val repository = mockk<BayRepository>()
    private val sessionManager = mockk<SessionManager>()

    private fun session(permissions: List<Permission> = emptyList()) = Session(
        userId = "u1", email = "a@b.c", fullName = "Ana", tenantId = "t1", tenantName = "Taller",
        membershipId = "m1", permissions = permissions.map { it.value }.toSet(), plan = null, branchId = "b1"
    )

    private fun bay(status: BayStatus) = WorkBay("bay1", "b1", "Bahia 1", null, status, null)

    @Test
    fun `poll refresca cada 30 s mientras se recolecta`() = runTest {
        coEvery { sessionManager.getSession() } returns session(listOf(Permission.BAYS_READ))
        coEvery { repository.refreshBranchBays() } returns Result.success(Unit)

        val job = launch { PollBayBoardUseCase(repository, sessionManager)().collect { } }
        advanceTimeBy(61_000)
        runCurrent()
        job.cancel()

        coVerify(exactly = 3) { repository.refreshBranchBays() }
    }

    @Test
    fun `poll se detiene al cancelar la recoleccion`() = runTest {
        coEvery { sessionManager.getSession() } returns session(listOf(Permission.BAYS_READ))
        coEvery { repository.refreshBranchBays() } returns Result.success(Unit)

        val job = launch { PollBayBoardUseCase(repository, sessionManager)().collect { } }
        advanceTimeBy(31_000)
        job.cancel()
        advanceTimeBy(120_000)
        runCurrent()

        coVerify(exactly = 2) { repository.refreshBranchBays() }
    }

    @Test
    fun `poll sin permiso no llama al backend`() = runTest {
        coEvery { sessionManager.getSession() } returns session()

        val emitted = PollBayBoardUseCase(repository, sessionManager)().toList()

        assertTrue(emitted.isEmpty())
        coVerify(exactly = 0) { repository.refreshBranchBays() }
    }

    @Test
    fun `no reasigna a una bahia en mantenimiento`() = runTest {
        val result = ReassignBayUseCase(repository)("wo1", bay(BayStatus.MAINTENANCE))

        assertTrue(result.exceptionOrNull() is BayUnavailableException)
        coVerify(exactly = 0) { repository.reassignBay(any(), any()) }
    }

    @Test
    fun `reasigna a una bahia libre`() = runTest {
        coEvery { repository.reassignBay("wo1", "bay1") } returns Result.success(Unit)

        assertEquals(Result.success(Unit), ReassignBayUseCase(repository)("wo1", bay(BayStatus.AVAILABLE)))
    }
}
