package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.InvalidBayInputException
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BayManagementUseCasesTest {

    private val repository = mockk<BayRepository>()

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidBayInputException).field

    @Test
    fun `entradas invalidas se rechazan localmente`() = runTest {
        assertEquals("workOrderId", field(ReleaseBayUseCase(repository)("")))
        assertEquals("bayId", field(SetBayMaintenanceUseCase(repository)("", "Calibración")))
        assertEquals("reason", field(SetBayMaintenanceUseCase(repository)("bay1", "   ")))
        assertEquals("reason", field(SetBayMaintenanceUseCase(repository)("bay1", "x".repeat(1001))))
        assertEquals("bayId", field(RestoreBayUseCase(repository)("  ")))

        coVerify(exactly = 0) { repository.releaseBay(any()) }
        coVerify(exactly = 0) { repository.setBayMaintenance(any(), any()) }
        coVerify(exactly = 0) { repository.restoreBay(any()) }
    }

    @Test
    fun `liberar, mantenimiento y restaurar delegan al repositorio`() = runTest {
        val maintenanceBay = WorkBay("bay1", "b1", "Elevador 1", null, BayStatus.MAINTENANCE, null)
        val availableBay = maintenanceBay.copy(status = BayStatus.AVAILABLE)

        coEvery { repository.releaseBay("wo1") } returns Result.success(Unit)
        coEvery { repository.setBayMaintenance("bay1", "Calibración") } returns Result.success(maintenanceBay)
        coEvery { repository.restoreBay("bay1") } returns Result.success(availableBay)

        assertTrue(ReleaseBayUseCase(repository)("wo1").isSuccess)
        assertEquals(BayStatus.MAINTENANCE, SetBayMaintenanceUseCase(repository)("bay1", "  Calibración ").getOrThrow().status)
        assertEquals(BayStatus.AVAILABLE, RestoreBayUseCase(repository)("bay1").getOrThrow().status)
    }
}
