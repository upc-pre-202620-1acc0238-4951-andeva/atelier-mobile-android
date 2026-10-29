package com.andeva.atelier.mobile.features.tasks.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.AddPartToTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.CompleteTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.GetTaskDetailUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.PauseTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.RemovePartFromTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SearchPartsUseCase
import com.andeva.atelier.mobile.features.tasks.application.StartTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SuggestLaborHoursUseCase
import com.andeva.atelier.mobile.features.tasks.application.UpdateTaskPartQuantityUseCase
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TaskDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<TaskRepository>()
    private val id = TaskId("t1")
    private val now = Instant.parse("2026-10-28T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val task = MutableStateFlow<Task?>(null)

    private fun tarea(
        status: TaskStatus = TaskStatus.ASSIGNED,
        hold: HoldReason? = null,
        startedAt: Instant? = null,
        paused: Long = 0
    ) = Task(
        id = id, workOrderId = "wo1", workOrderNumber = 10, workOrderStatus = null, vehicleId = "v1",
        plate = "ABC123", branchId = "b1", bayId = null, bayName = "B-1", serviceName = null, mechanicId = "m1",
        status = status, description = "Cambio de pastillas", estimatedHours = null, holdReason = hold,
        missingItemDescription = null, pendingSuspensionReason = null, startedAt = startedAt, totalPausedSeconds = paused
    )

    private fun producto(productId: String = "p1", quantity: String = "2") = TaskProduct(
        id = "tp-$productId", productId = productId, productName = "Pastillas", quantity = BigDecimal(quantity),
        unitPrice = Money(BigDecimal("50.00"), Money.PEN), totalAmount = Money(BigDecimal("100.00"), Money.PEN)
    )

    private fun repuesto(id: String = "p1", stock: String = "10") = PartOption(
        id = id, name = "Pastillas", sku = "BRK-001", category = "BRAKES",
        basePrice = BigDecimal("50.00"), totalStock = BigDecimal(stock), unitOfMeasure = "UNIT"
    )

    private fun detalle(
        status: TaskStatus,
        hold: HoldReason? = null,
        startedAt: Instant? = null,
        paused: Long = 0,
        products: List<TaskProduct> = emptyList()
    ) = TaskDetail(
        id = id, workOrderId = "wo1", serviceId = "s1", serviceName = "Frenos", mechanicId = "m1", mechanicName = null,
        status = status, description = "Cambio de pastillas", price = null, holdReason = hold,
        missingItemDescription = null, totalPausedSeconds = paused, startedAt = startedAt, completedAt = null,
        pendingSuspensionReason = null, pendingSuspensionRequestedAt = null, products = products, evidenceImages = emptyList()
    )

    private val mechanic = listOf(
        Permission.TASKS_READ, Permission.TASKS_TRACK_TIME, Permission.TASKS_COMPLETE, Permission.TASKS_CHARGE_PARTS
    )
    private val supervisor = mechanic + Permission.WORK_ORDERS_UPDATE

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "m@taller.pe", fullName = "Mecánico", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = mechanic): TaskDetailViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observeTask(id) } returns task
        return TaskDetailViewModel(
            observeSession,
            ObserveTaskUseCase(repository),
            GetTaskDetailUseCase(repository),
            StartTaskUseCase(repository),
            PauseTaskUseCase(repository),
            CompleteTaskUseCase(repository),
            SuggestLaborHoursUseCase(clock),
            SearchPartsUseCase(repository),
            AddPartToTaskUseCase(repository),
            UpdateTaskPartQuantityUseCase(repository),
            RemovePartFromTaskUseCase(repository),
            SavedStateHandle(mapOf("workOrderId" to "wo1", "taskId" to "t1"))
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun conDetalle(detail: TaskDetail) = coEvery { repository.getDetail(id) } returns Result.success(detail)

    @Test
    fun `carga la tarea de la cache y el detalle en linea`() = runTest(mainRule.dispatcher) {
        task.value = tarea()
        conDetalle(detalle(TaskStatus.ASSIGNED))
        val vm = viewModel()
        settle()

        assertNotNull(vm.uiState.value.task)
        assertEquals("Frenos", vm.uiState.value.detail?.serviceName)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `sin red muestra la tarea guardada y el aviso de conexion`() = runTest(mainRule.dispatcher) {
        task.value = tarea()
        coEvery { repository.getDetail(id) } returns Result.failure(IOException())
        val vm = viewModel()
        settle()

        assertNotNull(vm.uiState.value.task)
        assertNull(vm.uiState.value.detail)
        assertTrue(vm.uiState.value.error is UiText.Resource)
        assertEquals(TaskStatus.ASSIGNED, vm.uiState.value.status)
    }

    @Test
    fun `iniciar pasa la tarea a en curso con el inicio del cronometro`() = runTest(mainRule.dispatcher) {
        task.value = tarea()
        conDetalle(detalle(TaskStatus.ASSIGNED))
        coEvery { repository.start(id) } returns Result.success(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.canStart)

        vm.onStart()
        settle()

        assertEquals(TaskStatus.IN_PROGRESS, vm.uiState.value.status)
        assertEquals(now, vm.uiState.value.startedAt)
        assertNull(vm.uiState.value.busyAction)
        assertTrue(vm.uiState.value.canPause)
    }

    @Test
    fun `sin el permiso de control de tiempo no se puede iniciar`() = runTest(mainRule.dispatcher) {
        task.value = tarea()
        conDetalle(detalle(TaskStatus.ASSIGNED))
        val vm = viewModel(listOf(Permission.TASKS_READ))
        settle()

        assertFalse(vm.uiState.value.canStart)
        vm.onStart()
        settle()
        coVerify(exactly = 0) { repository.start(any()) }
    }

    @Test
    fun `una pausa por refrigerio permite reanudar con start`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        coEvery { repository.pause(id, PauseReason.BREAK) } returns
            Result.success(detalle(TaskStatus.ON_HOLD, HoldReason.BREAK, now, 0))
        coEvery { repository.start(id) } returns Result.success(detalle(TaskStatus.IN_PROGRESS, startedAt = now, paused = 600))
        val vm = viewModel()
        settle()

        vm.onPauseClick()
        assertNotNull(vm.uiState.value.pauseForm)
        vm.onPause(PauseReason.BREAK)
        settle()
        assertNull(vm.uiState.value.pauseForm)
        assertTrue(vm.uiState.value.isPausedByMechanic)
        assertTrue(vm.uiState.value.canStart)

        vm.onStart()
        settle()
        assertEquals(TaskStatus.IN_PROGRESS, vm.uiState.value.status)
        assertEquals(600L, vm.uiState.value.totalPausedSeconds)
    }

    @Test
    fun `el fin de turno anticipado muestra el mensaje del backend`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        coEvery { repository.pause(id, PauseReason.END_OF_SHIFT) } returns Result.failure(
            ApiException(422, "UNPROCESSABLE_ENTITY", "Tu turno aún no termina.")
        )
        val vm = viewModel()
        settle()

        vm.onPauseClick()
        vm.onPause(PauseReason.END_OF_SHIFT)
        settle()

        assertEquals("Tu turno aún no termina.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertEquals(TaskStatus.IN_PROGRESS, vm.uiState.value.status)
        assertNull(vm.uiState.value.busyAction)
    }

    @Test
    fun `una suspension por repuesto no se reanuda desde el mecanico`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.ON_HOLD, HoldReason.WAITING_PARTS)
        conDetalle(detalle(TaskStatus.ON_HOLD, HoldReason.WAITING_PARTS))
        val vm = viewModel()
        settle()

        assertTrue(vm.uiState.value.isSuspended)
        assertFalse(vm.uiState.value.canStart)
    }

    @Test
    fun `terminar precarga las horas sugeridas y cierra la tarea`() = runTest(mainRule.dispatcher) {
        val started = now.minusSeconds(2 * 3600 + 600)
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = started, paused = 600)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = started, paused = 600))
        coEvery { repository.complete(id, BigDecimal("2"), null) } returns Result.success(detalle(TaskStatus.COMPLETED))
        val vm = viewModel()
        settle()

        vm.onCompleteClick()
        assertEquals("2", vm.uiState.value.completeForm?.hours)

        vm.onCompleteConfirm()
        settle()
        assertTrue(vm.uiState.value.completed)
        assertNull(vm.uiState.value.completeForm)
    }

    @Test
    fun `horas invalidas marcan el campo sin llamar al backend`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        val vm = viewModel()
        settle()

        vm.onCompleteClick()
        vm.onHoursChange("abc")
        vm.onCompleteConfirm()
        settle()

        assertEquals("actualLaborHours", vm.uiState.value.invalidField)
        assertFalse(vm.uiState.value.completed)
        coVerify(exactly = 0) { repository.complete(any(), any(), any()) }
    }

    @Test
    fun `acepta la coma como separador decimal al cerrar`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        coEvery { repository.complete(id, BigDecimal("1.5"), "Sin novedad") } returns Result.success(detalle(TaskStatus.COMPLETED))
        val vm = viewModel()
        settle()

        vm.onCompleteClick()
        vm.onHoursChange("1,5")
        vm.onNotesChange(" Sin novedad ")
        vm.onCompleteConfirm()
        settle()

        assertTrue(vm.uiState.value.completed)
    }

    // --- US32 E2/E3: imputar repuesto ---

    @Test
    fun `imputar un repuesto recarga los productos de la tarea`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        coEvery { repository.getDetail(id) } returnsMany listOf(
            Result.success(detalle(TaskStatus.IN_PROGRESS, startedAt = now)),
            Result.success(detalle(TaskStatus.IN_PROGRESS, startedAt = now, products = listOf(producto())))
        )
        coEvery { repository.searchParts("pastillas") } returns Result.success(listOf(repuesto()))
        coEvery { repository.addPart(id, "p1", BigDecimal("2")) } returns Result.success(producto())
        val vm = viewModel()
        settle()

        vm.onChargeClick()
        vm.onChargeQueryChange("pastillas")
        settle()
        assertEquals(listOf("p1"), vm.uiState.value.chargeForm?.results?.map { it.id })

        vm.onPartSelect(repuesto())
        vm.onChargeQuantityChange("2")
        vm.onChargeConfirm()
        settle()

        assertNull(vm.uiState.value.chargeForm)
        assertEquals(1, vm.uiState.value.products.size)
        assertNull(vm.uiState.value.busyAction)
    }

    @Test
    fun `stock insuficiente muestra el 409 del backend y deja abierto el formulario`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        coEvery { repository.addPart(id, "p1", BigDecimal("50")) } returns Result.failure(
            ApiException(409, "CONFLICT", "Insufficient stock for SKU: requested 50, available 10")
        )
        val vm = viewModel()
        settle()

        vm.onChargeClick()
        vm.onPartSelect(repuesto())
        vm.onChargeQuantityChange("50")
        vm.onChargeConfirm()
        settle()

        assertEquals(
            "Insufficient stock for SKU: requested 50, available 10",
            (vm.uiState.value.error as UiText.Dynamic).value
        )
        assertNotNull(vm.uiState.value.chargeForm)
        assertTrue(vm.uiState.value.products.isEmpty())
    }

    @Test
    fun `un repuesto agotado no se puede elegir`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        val vm = viewModel()
        settle()

        vm.onChargeClick()
        vm.onPartSelect(repuesto(stock = "0"))

        assertNull(vm.uiState.value.chargeForm?.selected)
    }

    @Test
    fun `cantidad invalida al imputar marca el campo sin llamar al backend`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        val vm = viewModel()
        settle()

        vm.onChargeClick()
        vm.onPartSelect(repuesto())
        vm.onChargeQuantityChange("dos")
        vm.onChargeConfirm()
        settle()

        assertEquals("quantity", vm.uiState.value.invalidField)
        coVerify(exactly = 0) { repository.addPart(any(), any(), any()) }
    }

    @Test
    fun `cantidad cero la rechaza el caso de uso antes de llamar`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        val vm = viewModel()
        settle()

        vm.onChargeClick()
        vm.onPartSelect(repuesto())
        vm.onChargeQuantityChange("0")
        vm.onChargeConfirm()
        settle()

        assertEquals("quantity", vm.uiState.value.invalidField)
        coVerify(exactly = 0) { repository.addPart(any(), any(), any()) }
    }

    @Test
    fun `sin permiso de imputar no se abre el formulario`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        val vm = viewModel(listOf(Permission.TASKS_READ, Permission.TASKS_TRACK_TIME))
        settle()

        vm.onChargeClick()
        assertNull(vm.uiState.value.chargeForm)
    }

    // --- US33: devolver y corregir ---

    @Test
    fun `devolver un repuesto lo quita de la tarea`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        coEvery { repository.getDetail(id) } returnsMany listOf(
            Result.success(detalle(TaskStatus.IN_PROGRESS, startedAt = now, products = listOf(producto()))),
            Result.success(detalle(TaskStatus.IN_PROGRESS, startedAt = now))
        )
        coEvery { repository.removePart(id, "p1") } returns Result.success(Unit)
        val vm = viewModel(supervisor)
        settle()
        assertEquals(1, vm.uiState.value.products.size)

        vm.onReturnPartClick(vm.uiState.value.products.first())
        assertEquals(PartEditMode.RETURN, vm.uiState.value.partEdit?.mode)
        vm.onPartEditConfirm()
        settle()

        assertNull(vm.uiState.value.partEdit)
        assertTrue(vm.uiState.value.products.isEmpty())
        coVerify(exactly = 1) { repository.removePart(id, "p1") }
    }

    @Test
    fun `solo con permiso de orden se puede devolver`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now, products = listOf(producto())))
        val vm = viewModel(mechanic)
        settle()

        vm.onReturnPartClick(vm.uiState.value.products.first())
        assertNull(vm.uiState.value.partEdit)
        assertFalse(vm.uiState.value.canEditParts)
    }

    @Test
    fun `corregir la cantidad usa el detalle recargado`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now, products = listOf(producto(quantity = "2"))))
        coEvery { repository.updatePartQuantity(id, "p1", BigDecimal("1")) } returns Result.success(
            detalle(TaskStatus.IN_PROGRESS, startedAt = now, products = listOf(producto(quantity = "1")))
        )
        val vm = viewModel(supervisor)
        settle()

        vm.onEditPartClick(vm.uiState.value.products.first())
        assertEquals("2", vm.uiState.value.partEdit?.quantity)
        vm.onPartQuantityChange("1")
        vm.onPartEditConfirm()
        settle()

        assertNull(vm.uiState.value.partEdit)
        assertEquals(BigDecimal("1"), vm.uiState.value.products.first().quantity)
    }

    @Test
    fun `un error al devolver muestra el mensaje y conserva el formulario`() = runTest(mainRule.dispatcher) {
        task.value = tarea(TaskStatus.IN_PROGRESS, startedAt = now)
        conDetalle(detalle(TaskStatus.IN_PROGRESS, startedAt = now, products = listOf(producto())))
        coEvery { repository.removePart(id, "p1") } returns Result.failure(
            ApiException(403, "FORBIDDEN", "No puedes retirar este repuesto.")
        )
        val vm = viewModel(supervisor)
        settle()

        vm.onReturnPartClick(vm.uiState.value.products.first())
        vm.onPartEditConfirm()
        settle()

        assertEquals("No puedes retirar este repuesto.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertNotNull(vm.uiState.value.partEdit)
        assertEquals(1, vm.uiState.value.products.size)
    }
}
