package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.application.RefreshMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
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

class MyTasksViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<TaskRepository>()
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private fun tarea(id: String, status: TaskStatus = TaskStatus.ASSIGNED) = Task(
        id = TaskId(id), workOrderId = "wo1", workOrderNumber = 10, workOrderStatus = null, vehicleId = "v1",
        plate = "ABC123", branchId = "b1", bayId = null, bayName = "B-1", serviceName = null, mechanicId = "m1",
        status = status, description = "Cambio de pastillas", estimatedHours = null, holdReason = null,
        missingItemDescription = null, pendingSuspensionReason = null, startedAt = null, totalPausedSeconds = 0
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "m@taller.pe", fullName = "Mecánico", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(session: Session? = sessionWith(listOf(Permission.TASKS_READ))): MyTasksViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(session)
        every { repository.observeMyTasks() } returns tasks
        return MyTasksViewModel(observeSession, ObserveMyTasksUseCase(repository), RefreshMyTasksUseCase(repository), connectivity)
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `muestra las tareas de la cache y recarga al abrir`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returns Result.success(Unit)
        tasks.value = listOf(tarea("t1", TaskStatus.IN_PROGRESS), tarea("t2"))
        val vm = viewModel()
        settle()

        assertEquals(listOf("t1", "t2"), vm.uiState.value.tasks.map { it.id.value })
        assertTrue(vm.uiState.value.isLoaded)
        assertFalse(vm.uiState.value.isRefreshing)
        assertFalse(vm.uiState.value.showingCache)
        coVerify(exactly = 1) { repository.refreshMyTasks() }
    }

    @Test
    fun `sin red conserva las tareas guardadas y avisa`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returns Result.failure(IOException())
        tasks.value = listOf(tarea("t1"))
        val vm = viewModel()
        settle()

        assertEquals(1, vm.uiState.value.tasks.size)
        assertTrue(vm.uiState.value.showingCache)
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `un error del backend muestra su mensaje y reintentar lo limpia`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returnsMany listOf(
            Result.failure(ApiException(500, "SERVER_ERROR", "Servicio no disponible.")),
            Result.success(Unit)
        )
        val vm = viewModel()
        settle()
        assertEquals("Servicio no disponible.", (vm.uiState.value.error as UiText.Dynamic).value)

        vm.refresh()
        settle()
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.showingCache)
    }

    @Test
    fun `sin el permiso de lectura marca la pantalla como no permitida`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returns Result.success(Unit)
        val vm = viewModel(sessionWith(listOf(Permission.PARTS_READ)))
        settle()
        assertFalse(vm.uiState.value.canRead)
    }

    @Test
    fun `con el permiso de lectura la pantalla esta habilitada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returns Result.success(Unit)
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.canRead)
    }

    @Test
    fun `una cache vacia queda cargada y sin tareas`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returns Result.success(Unit)
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.isLoaded)
        assertTrue(vm.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `al volver la red recarga sola y quita el aviso de sin conexion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returnsMany listOf(Result.failure(IOException()), Result.success(Unit))
        online.value = false
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.showingCache)

        online.value = true
        settle()

        assertFalse(vm.uiState.value.showingCache)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 2) { repository.refreshMyTasks() }
    }

    @Test
    fun `con red desde el inicio no recarga una segunda vez`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshMyTasks() } returns Result.success(Unit)
        viewModel()
        settle()
        online.value = false
        online.value = true
        settle()
        coVerify(exactly = 1) { repository.refreshMyTasks() }
    }
}
