package com.andeva.atelier.mobile.features.workorders.presentation.services

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.CreateServiceUseCase
import com.andeva.atelier.mobile.features.workorders.application.DeleteServiceUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateServiceUseCase
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
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
import java.math.BigDecimal

class ServicesCatalogViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val observeSession = mockk<ObserveSessionUseCase>()
    private val connectivity = mockk<ConnectivityObserver>()

    private val s1 = ServiceOption("s1", "Cambio de aceite y filtro", Money.of("120.00", Money.PEN), 45)
    private val s2 = ServiceOption("s2", "Revisión del sistema eléctrico y alternador", Money.of("110.00", Money.PEN), 45)

    private fun session(permissions: Set<String>) = Session(
        userId = "u1",
        email = "admin@taller.pe",
        fullName = "Pedro Suárez",
        tenantId = "t1",
        tenantName = "Taller Suárez",
        membershipId = "m1",
        permissions = permissions,
        plan = null,
        branchId = "b1"
    )

    private fun createViewModel(
        permissions: Set<String> = setOf(Permission.SERVICES_READ.value, Permission.SERVICES_MANAGE.value),
        online: Boolean = true
    ): ServicesCatalogViewModel {
        every { observeSession() } returns flowOf(session(permissions))
        every { connectivity.isOnline } returns MutableStateFlow(online)
        return ServicesCatalogViewModel(
            observeSession = observeSession,
            refreshServices = RefreshServiceCatalogUseCase(repository),
            createService = CreateServiceUseCase(repository),
            updateService = UpdateServiceUseCase(repository),
            deleteService = DeleteServiceUseCase(repository),
            connectivity = connectivity
        )
    }

    @Test
    fun `carga servicios y filtra por nombre`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshServiceCatalog() } returns Result.success(listOf(s1, s2))
        val vm = createViewModel()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.uiState.value.services.size)
        vm.onSearchQueryChange("alternador")
        assertEquals(listOf(s2), vm.uiState.value.filteredServices)
    }

    @Test
    fun `crea un servicio nuevo y actualiza el listado`() = runTest(mainRule.dispatcher) {
        val created = ServiceOption("s3", "Afinamiento de motor", Money.of("220.00", Money.PEN), 90)
        coEvery { repository.refreshServiceCatalog() } returns Result.success(listOf(s1))
        coEvery {
            repository.createService("Afinamiento de motor", BigDecimal("220"), Money.PEN, 90)
        } returns Result.success(created)

        val vm = createViewModel()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        vm.onOpenCreate()
        vm.onNameChange("  Afinamiento de motor ")
        vm.onBasePriceChange("220")
        vm.onEstimatedMinutesChange("90")
        vm.onConfirmForm()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.form)
        assertTrue(vm.uiState.value.services.any { it.id == "s3" })
        coVerify(exactly = 1) { repository.createService("Afinamiento de motor", BigDecimal("220"), Money.PEN, 90) }
    }

    @Test
    fun `edita un servicio existente`() = runTest(mainRule.dispatcher) {
        val updated = s1.copy(basePrice = Money.of("135.00", Money.PEN), estimatedMinutes = 50)
        coEvery { repository.refreshServiceCatalog() } returns Result.success(listOf(s1))
        coEvery {
            repository.updateService("s1", "Cambio de aceite y filtro", BigDecimal("135"), Money.PEN, 50)
        } returns Result.success(updated)

        val vm = createViewModel()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        vm.onOpenEdit(s1)
        vm.onBasePriceChange("135")
        vm.onEstimatedMinutesChange("50")
        vm.onConfirmForm()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.form)
        assertEquals(Money.of("135.00", Money.PEN), vm.uiState.value.services.single().basePrice)
    }

    @Test
    fun `elimina un servicio libre y muestra error si esta en uso`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshServiceCatalog() } returns Result.success(listOf(s1, s2))
        coEvery { repository.deleteService("s2") } returns Result.success(Unit)
        coEvery { repository.deleteService("s1") } returns Result.failure(
            ApiException(409, "CONFLICT", "No se puede eliminar este servicio porque está asociado a tareas.")
        )

        val vm = createViewModel()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        vm.onOpenDelete(s2)
        vm.onConfirmDelete()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(s1), vm.uiState.value.services)

        vm.onOpenDelete(s1)
        vm.onConfirmDelete()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            UiText.Dynamic("No se puede eliminar este servicio porque está asociado a tareas."),
            vm.uiState.value.error
        )
        assertEquals(1, vm.uiState.value.services.size)
    }

    @Test
    fun `sin permiso de gestion no puede abrir alta ni edicion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshServiceCatalog() } returns Result.success(listOf(s1))
        val vm = createViewModel(permissions = setOf(Permission.SERVICES_READ.value))
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.canRead)
        assertFalse(vm.uiState.value.canManage)
        vm.onOpenCreate()
        vm.onOpenEdit(s1)
        assertNull(vm.uiState.value.form)
    }
}
