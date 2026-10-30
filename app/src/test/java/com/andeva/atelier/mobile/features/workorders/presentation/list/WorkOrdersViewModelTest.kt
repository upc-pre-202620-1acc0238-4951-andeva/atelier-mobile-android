package com.andeva.atelier.mobile.features.workorders.presentation.list

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class WorkOrdersViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val orders = MutableStateFlow<List<WorkOrder>>(emptyList())
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private fun orden(id: String, number: Int) = WorkOrder(
        id = id, internalNumber = number, branchId = "b1", vehicleId = "v1", customerId = "c1", currentBayId = null,
        bayName = null, status = WorkOrderStatus.IN_PROGRESS, diagnosticSummary = null, mileageIn = null,
        subtotal = null, taxAmount = null, totalAmount = null, plate = "ABC123", customerName = "Ana"
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "d@taller.pe", fullName = "Dueño", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = listOf(Permission.WORK_ORDERS_READ)): WorkOrdersViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observeBranchWorkOrders() } returns orders
        return WorkOrdersViewModel(
            observeSession,
            ObserveBranchWorkOrdersUseCase(repository),
            RefreshBranchWorkOrdersUseCase(repository),
            connectivity
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `muestra las ordenes de la cache y recarga al abrir`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBranchWorkOrders() } returns Result.success(Unit)
        orders.value = listOf(orden("o1", 1), orden("o2", 2))
        val vm = viewModel()
        settle()

        assertEquals(listOf("o1", "o2"), vm.uiState.value.orders.map { it.id })
        assertTrue(vm.uiState.value.isLoaded)
        assertFalse(vm.uiState.value.showingCache)
        coVerify(exactly = 1) { repository.refreshBranchWorkOrders() }
    }

    @Test
    fun `sin red conserva las ordenes guardadas y avisa`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBranchWorkOrders() } returns Result.failure(IOException())
        orders.value = listOf(orden("o1", 1))
        val vm = viewModel()
        settle()

        assertEquals(1, vm.uiState.value.orders.size)
        assertTrue(vm.uiState.value.showingCache)
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `un error del backend muestra su mensaje`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBranchWorkOrders() } returns
            Result.failure(ApiException(403, "FORBIDDEN", "No puedes ver las órdenes."))
        val vm = viewModel()
        settle()

        assertEquals("No puedes ver las órdenes.", (vm.uiState.value.error as UiText.Dynamic).value)
    }

    @Test
    fun `al volver la red recarga sola y quita el aviso`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBranchWorkOrders() } returnsMany listOf(Result.failure(IOException()), Result.success(Unit))
        online.value = false
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.showingCache)

        online.value = true
        settle()

        assertFalse(vm.uiState.value.showingCache)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 2) { repository.refreshBranchWorkOrders() }
    }

    @Test
    fun `sin el permiso de lectura marca la pantalla como no permitida`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBranchWorkOrders() } returns Result.success(Unit)
        val vm = viewModel(listOf(Permission.TASKS_READ))
        settle()
        assertFalse(vm.uiState.value.canRead)
    }

    @Test
    fun `sin ordenes queda cargada y vacia`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBranchWorkOrders() } returns Result.success(Unit)
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.isLoaded)
        assertTrue(vm.uiState.value.orders.isEmpty())
    }
}
