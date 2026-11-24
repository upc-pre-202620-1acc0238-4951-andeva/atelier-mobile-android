package com.andeva.atelier.mobile.features.bays.presentation

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.bays.application.CreateBayUseCase
import com.andeva.atelier.mobile.features.bays.application.ObserveBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.ReassignBayUseCase
import com.andeva.atelier.mobile.features.bays.application.RefreshBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.ReleaseBayUseCase
import com.andeva.atelier.mobile.features.bays.application.RestoreBayUseCase
import com.andeva.atelier.mobile.features.bays.application.SetBayMaintenanceUseCase
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration

class BaysViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val bayRepository = mockk<BayRepository>()
    private val workOrderRepository = mockk<WorkOrderRepository>()
    private val boardFlow = MutableStateFlow<List<BayBoardItem>>(emptyList())
    private val ordersFlow = MutableStateFlow<List<WorkOrder>>(emptyList())
    private val onlineFlow = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns onlineFlow }

    private val occupiedItem = BayBoardItem(
        WorkBay("bay1", "b1", "Elevador 1", BayType.LIFT, BayStatus.OCCUPIED, "wo1"),
        BayOccupant("wo1", 1041, "F5U731", listOf("m1"), Duration.ofMinutes(45))
    )
    private val availableItem = BayBoardItem(
        WorkBay("bay2", "b1", "Foso 2", BayType.DIAGNOSTIC_PIT, BayStatus.AVAILABLE, null),
        null
    )
    private val maintenanceItem = BayBoardItem(
        WorkBay("bay3", "b1", "Alineación", BayType.ALIGNMENT_STATION, BayStatus.MAINTENANCE, null),
        null
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u1", email = "jefe@taller.pe", fullName = "Jefe", tenantId = "t1", tenantName = "Taller",
        membershipId = "m1", permissions = permissions.map { it.value }.toSet(), plan = null, branchId = "b1"
    )

    private fun viewModel(
        permissions: List<Permission> = listOf(
            Permission.BAYS_READ, Permission.BAYS_REASSIGN, Permission.BAYS_MANAGE, Permission.WORK_ORDERS_UPDATE
        )
    ): BaysViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { bayRepository.observeBoard() } returns boardFlow
        every { workOrderRepository.observeBranchWorkOrders() } returns ordersFlow
        coEvery { bayRepository.refreshBranchBays() } returns Result.success(Unit)
        coEvery { workOrderRepository.refreshBranchWorkOrders() } returns Result.success(Unit)
        return BaysViewModel(
            observeSession = observeSession,
            observeBayBoard = ObserveBayBoardUseCase(bayRepository),
            observeOrders = ObserveBranchWorkOrdersUseCase(workOrderRepository),
            refreshBayBoard = RefreshBayBoardUseCase(bayRepository),
            refreshOrders = RefreshBranchWorkOrdersUseCase(workOrderRepository),
            reassignBay = ReassignBayUseCase(bayRepository),
            createBayUseCase = CreateBayUseCase(bayRepository),
            connectivity = connectivity,
            releaseBay = ReleaseBayUseCase(bayRepository),
            setBayMaintenance = SetBayMaintenanceUseCase(bayRepository),
            restoreBay = RestoreBayUseCase(bayRepository)
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `filtra por disponible ocupada y mantenimiento`() = runTest(mainRule.dispatcher) {
        boardFlow.value = listOf(occupiedItem, availableItem, maintenanceItem)
        val vm = viewModel()
        settle()

        assertEquals(3, vm.uiState.value.filteredBoard.size)
        vm.onFilterChange(BayStatus.MAINTENANCE)
        assertEquals(listOf("bay3"), vm.uiState.value.filteredBoard.map { it.bay.id })
        assertEquals(1, vm.uiState.value.maintenanceCount)
    }

    @Test
    fun `liberar bahia ocupada cierra la hoja y muestra aviso`() = runTest(mainRule.dispatcher) {
        boardFlow.value = listOf(occupiedItem, availableItem)
        coEvery { bayRepository.releaseBay("wo1") } returns Result.success(Unit)
        val vm = viewModel()
        settle()

        vm.onOpenBayActions(occupiedItem)
        vm.onReleaseSelectedBay()
        settle()

        coVerify { bayRepository.releaseBay("wo1") }
        assertNull(vm.uiState.value.selectedBayItem)
        assertNotNull(vm.uiState.value.notice)
    }

    @Test
    fun `poner en mantenimiento y restaurar bahia`() = runTest(mainRule.dispatcher) {
        boardFlow.value = listOf(availableItem, maintenanceItem)
        coEvery { bayRepository.setBayMaintenance("bay2", "Mantenimiento preventivo") } returns Result.success(
            availableItem.bay.copy(status = BayStatus.MAINTENANCE)
        )
        coEvery { bayRepository.restoreBay("bay3") } returns Result.success(
            maintenanceItem.bay.copy(status = BayStatus.AVAILABLE)
        )
        val vm = viewModel()
        settle()

        vm.onOpenBayActions(availableItem)
        vm.onToggleMaintenanceInput(true)
        vm.onMaintenanceReasonChange("Mantenimiento preventivo")
        vm.onConfirmSetMaintenance()
        settle()

        coVerify { bayRepository.setBayMaintenance("bay2", "Mantenimiento preventivo") }
        assertNull(vm.uiState.value.selectedBayItem)
        assertTrue(vm.uiState.value.notice != null)

        vm.onOpenBayActions(maintenanceItem)
        vm.onRestoreSelectedBay()
        settle()

        coVerify { bayRepository.restoreBay("bay3") }
        assertNull(vm.uiState.value.selectedBayItem)
    }
}
