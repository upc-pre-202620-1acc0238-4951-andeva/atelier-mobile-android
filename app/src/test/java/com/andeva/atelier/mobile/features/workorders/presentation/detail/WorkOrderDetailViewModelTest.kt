package com.andeva.atelier.mobile.features.workorders.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class WorkOrderDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private fun tarea(id: String, status: TaskStatus, mechanic: String? = "Luis Mamani") = TaskDetail(
        id = TaskId(id), workOrderId = "wo1", serviceId = "s1", serviceName = "Diagnóstico OBD-II", mechanicId = "m1",
        mechanicName = mechanic, status = status, description = "Lectura de códigos", price = null, holdReason = null,
        missingItemDescription = null, totalPausedSeconds = 0, startedAt = null, completedAt = null,
        pendingSuspensionReason = null, pendingSuspensionRequestedAt = null, products = emptyList(), evidenceImages = emptyList()
    )

    private fun detalle(vararg tasks: TaskDetail) = WorkOrderDetail(
        workOrder = WorkOrder(
            id = "wo1", internalNumber = 1, branchId = "b1", vehicleId = "v1", customerId = "c1", currentBayId = null,
            bayName = null, status = WorkOrderStatus.IN_PROGRESS, diagnosticSummary = null, mileageIn = null,
            subtotal = null, taxAmount = null, totalAmount = null
        ),
        tasks = tasks.toList(), proposals = emptyList(), intakeImages = emptyList(), createdAt = null, updatedAt = null
    )

    private fun viewModel() = WorkOrderDetailViewModel(
        GetWorkOrderDetailUseCase(repository), connectivity, SavedStateHandle(mapOf("workOrderId" to "wo1"))
    )

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `carga el detalle con las tareas de la orden`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns
            Result.success(detalle(tarea("t1", TaskStatus.IN_PROGRESS), tarea("t2", TaskStatus.ASSIGNED)))
        val vm = viewModel()
        settle()

        assertEquals(listOf("t1", "t2"), vm.uiState.value.detail?.tasks?.map { it.id.value })
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `un error del backend muestra su mensaje y se puede reintentar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returnsMany listOf(
            Result.failure(ApiException(404, "NOT_FOUND", "La orden no existe.")),
            Result.success(detalle(tarea("t1", TaskStatus.ASSIGNED)))
        )
        val vm = viewModel()
        settle()
        assertEquals("La orden no existe.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertNull(vm.uiState.value.detail)

        vm.load()
        settle()
        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.detail)
    }

    @Test
    fun `sin red muestra el aviso y al volver la red carga sola`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returnsMany listOf(
            Result.failure(IOException()),
            Result.success(detalle(tarea("t1", TaskStatus.ASSIGNED)))
        )
        online.value = false
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
        assertNull(vm.uiState.value.detail)

        online.value = true
        settle()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.detail?.tasks?.size)
        coVerify(exactly = 2) { repository.getDetail("wo1") }
    }

    @Test
    fun `una orden sin tareas queda con la lista vacia`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.detail?.tasks?.isEmpty() == true)
    }
}
