package com.andeva.atelier.mobile.features.workorders.presentation.open

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.AddWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.application.FindDraftWorkOrderForVehicleUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetBranchMechanicsUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.OpenWorkOrderUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateWorkOrderUseCase
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class OpenWorkOrderViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val aceite = ServiceOption("s1", "Cambio de aceite", null, 45)
    private val frenos = ServiceOption("s2", "Revisión de frenos", null, 60)
    private val mecanico = MechanicOption("m1", "Mecánico", "ACTIVE")

    private fun orden(id: String) = mockk<WorkOrder> { every { this@mockk.id } returns id }

    private val allPermissions = listOf(Permission.WORK_ORDERS_CREATE, Permission.WORK_ORDERS_UPDATE)

    private fun sessionWith(permissions: List<Permission>, branchId: String? = "b1") = Session(
        userId = "u", email = "a@taller.pe", fullName = "Asesor", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null, branchId = branchId
    )

    private fun viewModel(
        permissions: List<Permission> = allPermissions,
        workOrderId: String? = null,
        branchId: String? = "b1"
    ): OpenWorkOrderViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions, branchId))
        coEvery { repository.getServiceCatalog() } returns Result.success(listOf(aceite, frenos))
        coEvery { repository.getBranchMechanics() } returns Result.success(listOf(mecanico))
        val args = buildMap<String, Any?> {
            put("vehicleId", "v1")
            put("customerId", "c1")
            if (workOrderId != null) put("workOrderId", workOrderId)
        }
        return OpenWorkOrderViewModel(
            observeSession,
            OpenWorkOrderUseCase(repository),
            UpdateWorkOrderUseCase(repository),
            AddWorkOrderTaskUseCase(repository),
            FindDraftWorkOrderForVehicleUseCase(repository),
            GetServiceCatalogUseCase(repository),
            GetBranchMechanicsUseCase(repository),
            SavedStateHandle(args)
        )
    }

    private fun OpenWorkOrderViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun OpenWorkOrderViewModel.fill() {
        onMileageChange("42 000km")
        onFuelChange(FuelLevel.HALF)
        onReasonChange(" Ruido al frenar ")
    }

    private fun sinBorrador() = coEvery { repository.findDraftForVehicle("b1", "v1") } returns Result.success(null)

    @Test
    fun `carga los servicios y los mecanicos de la sede`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val vm = viewModel()
        vm.settle()

        assertEquals(listOf(aceite, frenos), vm.uiState.value.services)
        assertEquals(listOf(mecanico), vm.uiState.value.mechanics)
        assertFalse(vm.uiState.value.isLoadingCatalog)
    }

    @Test
    fun `el kilometraje solo acepta digitos`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val vm = viewModel()
        vm.settle()
        vm.onMileageChange("42 000km")
        assertEquals("42000", vm.uiState.value.mileage)
    }

    @Test
    fun `faltan kilometraje combustible o motivo y no se puede abrir`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val vm = viewModel()
        vm.settle()
        vm.onMileageChange("1000")
        vm.onFuelChange(FuelLevel.FULL)
        assertFalse(vm.uiState.value.canSubmit)

        vm.onReasonChange("Revisión")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `abre una orden nueva con el combustible al inicio del diagnostico y agrega las faenas`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val summary = "Combustible: 1/2. Ruido al frenar"
        coEvery { repository.open(NewWorkOrder("v1", "c1", 42000, summary)) } returns Result.success(orden("wo1"))
        coEvery { repository.addTask(any(), any()) } returns Result.success(mockk<WorkOrderDetail>())
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onToggleService("s1")
        vm.onToggleService("s2")
        vm.onMechanicChange("m1")
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 1) { repository.open(NewWorkOrder("v1", "c1", 42000, summary)) }
        coVerify { repository.addTask("wo1", NewWorkOrderTask("s1", "Cambio de aceite", "m1")) }
        coVerify { repository.addTask("wo1", NewWorkOrderTask("s2", "Revisión de frenos", "m1")) }
        assertEquals("wo1", vm.uiState.value.doneWorkOrderId)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `una orden sin faenas tambien se abre`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        coEvery { repository.open(any()) } returns Result.success(orden("wo1"))
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 0) { repository.addTask(any(), any()) }
        assertEquals("wo1", vm.uiState.value.doneWorkOrderId)
    }

    @Test
    fun `si el vehiculo ya tiene un borrador lo completa en vez de crear otra orden`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findDraftForVehicle("b1", "v1") } returns Result.success(orden("draft1"))
        coEvery { repository.update("draft1", 42000, "Combustible: 1/2. Ruido al frenar") } returns Result.success(orden("draft1"))
        val vm = viewModel()
        vm.settle()
        assertTrue(vm.uiState.value.isCompletingDraft)

        vm.fill()
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 0) { repository.open(any()) }
        coVerify(exactly = 1) { repository.update("draft1", 42000, "Combustible: 1/2. Ruido al frenar") }
        assertEquals("draft1", vm.uiState.value.doneWorkOrderId)
    }

    @Test
    fun `la orden que llega en la ruta se completa sin buscar borrador`() = runTest(mainRule.dispatcher) {
        coEvery { repository.update("wo9", any(), any()) } returns Result.success(orden("wo9"))
        val vm = viewModel(workOrderId = "wo9")
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 0) { repository.findDraftForVehicle(any(), any()) }
        assertEquals("wo9", vm.uiState.value.doneWorkOrderId)
    }

    @Test
    fun `completar un borrador exige permiso de actualizar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findDraftForVehicle(any(), any()) } returns Result.success(orden("draft1"))
        val vm = viewModel(permissions = listOf(Permission.WORK_ORDERS_CREATE))
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        assertFalse(vm.uiState.value.hasPermission)
        coVerify(exactly = 0) { repository.update(any(), any(), any()) }
    }

    @Test
    fun `sin permiso de crear no se abre la orden`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val vm = viewModel(permissions = emptyList())
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 0) { repository.open(any()) }
    }

    @Test
    fun `un 409 muestra el mensaje del backend y no deja la orden guardada`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        coEvery { repository.open(any()) } returns Result.failure(ApiException(409, "CONFLICT", "El vehículo ya tiene una orden abierta."))
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        assertEquals("El vehículo ya tiene una orden abierta.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertFalse(vm.uiState.value.orderSaved)
        assertNull(vm.uiState.value.doneWorkOrderId)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `si falla una faena la orden queda guardada y al reintentar solo se agregan las que faltan`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        coEvery { repository.open(any()) } returns Result.success(orden("wo1"))
        coEvery { repository.addTask("wo1", match { it.serviceId == "s1" }) } returns Result.success(mockk<WorkOrderDetail>())
        coEvery { repository.addTask("wo1", match { it.serviceId == "s2" }) } returnsMany listOf(
            Result.failure(ApiException(500, "ERR", "No se pudo agregar.")),
            Result.success(mockk<WorkOrderDetail>())
        )
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onToggleService("s1")
        vm.onToggleService("s2")
        vm.onSubmit()
        vm.settle()

        assertTrue(vm.uiState.value.orderSaved)
        assertEquals(setOf("s1"), vm.uiState.value.addedServiceIds)
        assertNull(vm.uiState.value.doneWorkOrderId)
        assertEquals("No se pudo agregar.", (vm.uiState.value.error as UiText.Dynamic).value)

        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 1) { repository.open(any()) }
        coVerify(exactly = 1) { repository.addTask("wo1", match { it.serviceId == "s1" }) }
        coVerify(exactly = 2) { repository.addTask("wo1", match { it.serviceId == "s2" }) }
        assertEquals("wo1", vm.uiState.value.doneWorkOrderId)
    }

    @Test
    fun `con la orden guardada los datos de ingreso y las faenas agregadas no se editan`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        coEvery { repository.open(any()) } returns Result.success(orden("wo1"))
        coEvery { repository.addTask(any(), any()) } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onToggleService("s1")
        vm.onSubmit()
        vm.settle()

        vm.onMileageChange("99")
        vm.onReasonChange("otro")
        assertEquals("42000", vm.uiState.value.mileage)
        assertEquals(" Ruido al frenar ", vm.uiState.value.reason)
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `un motivo demasiado largo marca el campo sin guardar la orden`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val vm = viewModel()
        vm.settle()
        vm.onMileageChange("1000")
        vm.onFuelChange(FuelLevel.FULL)
        vm.onReasonChange("x".repeat(2100))
        vm.onSubmit()
        vm.settle()

        assertEquals("diagnosticSummary", vm.uiState.value.invalidField)
        assertFalse(vm.uiState.value.orderSaved)
        coVerify(exactly = 0) { repository.open(any()) }
    }

    @Test
    fun `se puede quitar una faena elegida y cambiar de mecanico`() = runTest(mainRule.dispatcher) {
        sinBorrador()
        val vm = viewModel()
        vm.settle()
        vm.onToggleService("s1")
        vm.onToggleService("s1")
        vm.onMechanicChange("m1")
        vm.onMechanicChange(null)

        assertTrue(vm.uiState.value.selectedServiceIds.isEmpty())
        assertNull(vm.uiState.value.selectedMechanicId)
    }
}
