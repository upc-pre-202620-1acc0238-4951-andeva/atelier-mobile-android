package com.andeva.atelier.mobile.features.reception.presentation.search

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.FindVehicleByPlateUseCase
import com.andeva.atelier.mobile.features.reception.application.GetCustomerVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.MarkAppointmentArrivedUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.SearchCustomersUseCase
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class VehicleSearchViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<ReceptionRepository>()
    private val workOrders = mockk<WorkOrderRepository>()
    private val agenda = MutableStateFlow<List<Appointment>>(emptyList())

    // 27-oct-2026 15:00 UTC es 10:00 en Lima: el mismo día.
    private val clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 27)

    private val ana = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", null, null, "ACTIVE")
    private val luis = Customer("c2", CustomerType.INDIVIDUAL, "Luis Ramos", "45678913", null, null, "ACTIVE")
    private val yaris = Vehicle("v1", "ABC123", null, "Toyota", "Yaris", 2019, EngineType.GASOLINE, "c1", null)
    private val cita = Appointment(
        "a1", "b1", "c1", "v1", Instant.parse("2026-10-27T15:00:00Z"), 60, null, AppointmentStatus.CONFIRMED, "ABC123", "Ana Torres"
    )

    private val allPermissions = listOf(
        Permission.VEHICLES_READ, Permission.CUSTOMERS_READ, Permission.APPOINTMENTS_READ,
        Permission.APPOINTMENTS_UPDATE, Permission.CUSTOMERS_CREATE, Permission.VEHICLES_CREATE,
        Permission.WORK_ORDERS_CREATE
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "r@taller.pe", fullName = "Recepción", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = allPermissions): VehicleSearchViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observeAppointments(today) } returns agenda
        coEvery { repository.refreshAppointments(today) } returns Result.success(Unit)
        return VehicleSearchViewModel(
            observeSession,
            FindVehicleByPlateUseCase(repository),
            SearchCustomersUseCase(repository),
            GetCustomerVehiclesUseCase(repository),
            RefreshAppointmentsUseCase(repository),
            ObserveAppointmentsUseCase(repository),
            MarkAppointmentArrivedUseCase(repository, workOrders),
            clock
        )
    }

    private fun VehicleSearchViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `los permisos de la sesion habilitan cada accion`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        val state = vm.uiState.value
        assertTrue(state.canSearch && state.canViewAppointments && state.canMarkArrival && state.canRegister)
        assertTrue(state.canOpenWorkOrder)

        val solo = viewModel(listOf(Permission.VEHICLES_READ, Permission.CUSTOMERS_READ))
        solo.settle()
        assertTrue(solo.uiState.value.canSearch)
        assertFalse(solo.uiState.value.canMarkArrival)
        assertFalse(solo.uiState.value.canRegister)
        assertFalse(solo.uiState.value.canOpenWorkOrder)
    }

    @Test
    fun `la agenda de hoy se carga una vez al conocer el permiso`() = runTest(mainRule.dispatcher) {
        agenda.value = listOf(cita)
        val vm = viewModel()
        vm.settle()

        coVerify(exactly = 1) { repository.refreshAppointments(today) }
        assertEquals(listOf(cita), vm.uiState.value.appointments)
        assertFalse(vm.uiState.value.isRefreshingAppointments)
    }

    @Test
    fun `sin permiso de citas no se pide la agenda`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(listOf(Permission.VEHICLES_READ, Permission.CUSTOMERS_READ))
        vm.settle()
        vm.refresh()
        vm.settle()

        coVerify(exactly = 0) { repository.refreshAppointments(any()) }
    }

    @Test
    fun `sin red la agenda sigue con la cache y no muestra error`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        coEvery { repository.refreshAppointments(today) } returns Result.failure(IOException("sin red"))
        vm.settle()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `buscar una placa existente muestra el vehiculo y su titular`() = runTest(mainRule.dispatcher) {
        // la normalizacion (mayusculas, sin espacios) la hace el repositorio, no el ViewModel
        coEvery { repository.findVehicleByPlate("abc 123") } returns Result.success(VehicleWithOwner(yaris, ana))
        val vm = viewModel()
        vm.settle()
        vm.onQueryChange("abc 123")
        vm.onSearch()
        vm.settle()

        assertEquals(VehicleWithOwner(yaris, ana), vm.uiState.value.vehicleResult)
        assertFalse(vm.uiState.value.vehicleNotFound)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `una placa sin registro lleva al alta`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findVehicleByPlate(any()) } returns Result.success(null)
        val vm = viewModel()
        vm.settle()
        vm.onQueryChange("XYZ999")
        vm.onSearch()
        vm.settle()

        assertTrue(vm.uiState.value.vehicleNotFound)
        assertNull(vm.uiState.value.vehicleResult)
    }

    @Test
    fun `un texto vacio no busca`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onQueryChange("   ")
        vm.onSearch()
        vm.settle()

        assertFalse(vm.uiState.value.canSubmitSearch)
        coVerify(exactly = 0) { repository.findVehicleByPlate(any()) }
    }

    @Test
    fun `sin permiso de lectura no se busca`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(listOf(Permission.APPOINTMENTS_READ))
        vm.settle()
        vm.onQueryChange("ABC123")
        vm.onSearch()
        vm.settle()

        coVerify(exactly = 0) { repository.findVehicleByPlate(any()) }
    }

    @Test
    fun `un unico cliente por documento carga sus vehiculos solo`() = runTest(mainRule.dispatcher) {
        coEvery { repository.searchCustomers("45678912") } returns Result.success(listOf(ana))
        coEvery { repository.getCustomerVehicles("c1") } returns Result.success(listOf(yaris))
        val vm = viewModel()
        vm.settle()
        vm.onModeChange(SearchMode.DOCUMENT)
        vm.onQueryChange(" 45678912 ")
        vm.onSearch()
        vm.settle()

        assertEquals(ana, vm.uiState.value.selectedCustomer)
        assertEquals(listOf(yaris), vm.uiState.value.customerVehicles)
        assertFalse(vm.uiState.value.isLoadingVehicles)
    }

    @Test
    fun `varios clientes esperan a que se elija uno`() = runTest(mainRule.dispatcher) {
        coEvery { repository.searchCustomers("4567") } returns Result.success(listOf(ana, luis))
        coEvery { repository.getCustomerVehicles("c2") } returns Result.success(emptyList())
        val vm = viewModel()
        vm.settle()
        vm.onModeChange(SearchMode.DOCUMENT)
        vm.onQueryChange("4567")
        vm.onSearch()
        vm.settle()

        assertNull(vm.uiState.value.selectedCustomer)
        coVerify(exactly = 0) { repository.getCustomerVehicles(any()) }

        vm.onSelectCustomer(luis)
        vm.settle()
        assertEquals(luis, vm.uiState.value.selectedCustomer)
        assertTrue(vm.uiState.value.customerVehicles.isEmpty())
    }

    @Test
    fun `un documento sin clientes lo indica`() = runTest(mainRule.dispatcher) {
        coEvery { repository.searchCustomers(any()) } returns Result.success(emptyList())
        val vm = viewModel()
        vm.settle()
        vm.onModeChange(SearchMode.DOCUMENT)
        vm.onQueryChange("99999999")
        vm.onSearch()
        vm.settle()

        assertTrue(vm.uiState.value.customersNotFound)
    }

    @Test
    fun `cambiar de modo o de texto descarta el resultado anterior`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findVehicleByPlate(any()) } returns Result.success(VehicleWithOwner(yaris, ana))
        val vm = viewModel()
        vm.settle()
        vm.onQueryChange("ABC123")
        vm.onSearch()
        vm.settle()
        vm.onQueryChange("ABC12")
        assertNull(vm.uiState.value.vehicleResult)
        assertFalse(vm.uiState.value.searched)

        vm.onSearch()
        vm.settle()
        vm.onModeChange(SearchMode.DOCUMENT)
        assertEquals("", vm.uiState.value.query)
        assertNull(vm.uiState.value.vehicleResult)
    }

    @Test
    fun `un error del backend muestra su mensaje`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findVehicleByPlate(any()) } returns Result.failure(ApiException(500, "ERR", "Servicio no disponible."))
        val vm = viewModel()
        vm.settle()
        vm.onQueryChange("ABC123")
        vm.onSearch()
        vm.settle()

        assertEquals("Servicio no disponible.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `marcar la llegada con la orden lista avisa que esta lista`() = runTest(mainRule.dispatcher) {
        val draft = mockk<WorkOrder> { every { id } returns "wo1" }
        coEvery { repository.markArrived("a1") } returns Result.success(cita)
        coEvery { workOrders.findDraftForVehicle("b1", "v1") } returns Result.success(draft)
        val vm = viewModel()
        vm.settle()
        vm.onMarkArrived("a1")
        vm.settle()

        val arrival = vm.uiState.value.arrival as ArrivalResult.WorkOrderReady
        assertEquals("wo1", arrival.workOrderId)
        assertNull(vm.uiState.value.arrivingId)
    }

    @Test
    fun `marcar la llegada sin orden todavia pide volver a consultar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.markArrived("a1") } returns Result.success(cita)
        coEvery { workOrders.findDraftForVehicle(any(), any()) } returns Result.success(null)
        val vm = viewModel()
        vm.settle()
        vm.onMarkArrived("a1")
        vm.settle()

        assertTrue(vm.uiState.value.arrival is ArrivalResult.WorkOrderPending)
        coVerify(exactly = MarkAppointmentArrivedUseCase.ATTEMPTS) { workOrders.findDraftForVehicle("b1", "v1") }
    }

    @Test
    fun `sin permiso de actualizar citas no se marca la llegada`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(listOf(Permission.APPOINTMENTS_READ))
        vm.settle()
        vm.onMarkArrived("a1")
        vm.settle()

        coVerify(exactly = 0) { repository.markArrived(any()) }
    }

    @Test
    fun `un error al marcar la llegada muestra el mensaje y libera el boton`() = runTest(mainRule.dispatcher) {
        coEvery { repository.markArrived("a1") } returns Result.failure(ApiException(409, "ERR", "La cita ya llegó."))
        val vm = viewModel()
        vm.settle()
        vm.onMarkArrived("a1")
        vm.settle()

        assertEquals("La cita ya llegó.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertNull(vm.uiState.value.arrivingId)
        assertNull(vm.uiState.value.arrival)
    }
}
