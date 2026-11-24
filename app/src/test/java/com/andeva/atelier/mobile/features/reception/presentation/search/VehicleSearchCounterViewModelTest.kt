package com.andeva.atelier.mobile.features.reception.presentation.search

import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.CancelAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.ConfirmAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.CreateAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.FindVehicleByPlateUseCase
import com.andeva.atelier.mobile.features.reception.application.GetCustomerVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.MarkAppointmentArrivedUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RescheduleAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.SearchCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.UpdateCustomerContactUseCase
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class VehicleSearchCounterViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<ReceptionRepository>()
    private val workOrders = mockk<WorkOrderRepository>()
    private val agenda = MutableStateFlow<List<Appointment>>(emptyList())
    private val clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 27)

    private val ana = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", "ana@correo.pe", "999888777", "ACTIVE")
    private val yaris = Vehicle("v1", "ABC123", null, "Toyota", "Yaris", 2019, EngineType.GASOLINE, "c1", null)
    private val pendingAppointment = Appointment(
        "a1", "b1", "c1", "v1", Instant.parse("2026-10-27T15:00:00Z"), 60, "Frenos",
        AppointmentStatus.PENDING, "ABC123", "Ana Torres"
    )

    private val permissions = listOf(
        Permission.VEHICLES_READ, Permission.CUSTOMERS_READ, Permission.CUSTOMERS_UPDATE,
        Permission.APPOINTMENTS_READ, Permission.APPOINTMENTS_CREATE, Permission.APPOINTMENTS_UPDATE
    )

    private fun sessionWith(perms: List<Permission>) = Session(
        userId = "u", email = "r@taller.pe", fullName = "Recepción", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = perms.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(perms: List<Permission> = permissions): VehicleSearchViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(perms))
        every { repository.observeAppointments(today) } returns agenda
        coEvery { repository.refreshAppointments(today) } returns Result.success(Unit)
        return VehicleSearchViewModel(
            observeSession = observeSession,
            findVehicleByPlate = FindVehicleByPlateUseCase(repository),
            searchCustomers = SearchCustomersUseCase(repository),
            getCustomerVehicles = GetCustomerVehiclesUseCase(repository),
            refreshAppointments = RefreshAppointmentsUseCase(repository),
            observeAppointments = ObserveAppointmentsUseCase(repository),
            markArrived = MarkAppointmentArrivedUseCase(repository, workOrders),
            clock = clock,
            createAppointment = CreateAppointmentUseCase(repository),
            confirmAppointment = ConfirmAppointmentUseCase(repository),
            rescheduleAppointment = RescheduleAppointmentUseCase(repository),
            cancelAppointment = CancelAppointmentUseCase(repository),
            updateCustomerContact = UpdateCustomerContactUseCase(repository)
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `permisos de citas y contacto habilitan botones correspondientes`() = runTest(mainRule.dispatcher) {
        val full = viewModel()
        settle()
        assertTrue(full.uiState.value.canCreateAppointments)
        assertTrue(full.uiState.value.canUpdateCustomer)

        val readOnly = viewModel(listOf(Permission.VEHICLES_READ, Permission.CUSTOMERS_READ))
        settle()
        assertFalse(readOnly.uiState.value.canCreateAppointments)
        assertFalse(readOnly.uiState.value.canUpdateCustomer)
    }

    @Test
    fun `agendar cita valida y envia fecha Lima en UTC`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getCustomerVehicles("c1") } returns Result.success(listOf(yaris))
        coEvery { repository.createAppointment("c1", "v1", any(), 60, "Revisión") } returns Result.success(pendingAppointment)

        val vm = viewModel()
        settle()

        vm.onOpenCreateAppointment()
        vm.onSelectAppointmentCustomer(ana)
        settle()
        vm.onAppointmentDateChange("2026-10-27")
        vm.onAppointmentTimeChange("10:00")
        vm.onAppointmentReasonChange("Revisión")
        vm.onConfirmCreateAppointment()
        settle()

        // 2026-10-27 10:00 America/Lima = 2026-10-27T15:00:00Z
        coVerify { repository.createAppointment("c1", "v1", Instant.parse("2026-10-27T15:00:00Z"), 60, "Revisión") }
        assertNull(vm.uiState.value.createAppointmentForm)
        assertNotNull(vm.uiState.value.notice)
    }

    @Test
    fun `confirmar reprogramar y cancelar cita actualizan el estado`() = runTest(mainRule.dispatcher) {
        agenda.value = listOf(pendingAppointment)
        coEvery { repository.confirmAppointment("a1") } returns Result.success(pendingAppointment.copy(status = AppointmentStatus.CONFIRMED))
        coEvery { repository.rescheduleAppointment("a1", any()) } returns Result.success(pendingAppointment)
        coEvery { repository.cancelAppointment("a1", "No puede asistir") } returns Result.success(pendingAppointment.copy(status = AppointmentStatus.CANCELED))

        val vm = viewModel()
        settle()

        vm.onOpenAppointmentActions("a1")
        vm.onConfirmAppointment("a1")
        settle()
        assertNull(vm.uiState.value.actionAppointmentId)
        coVerify { repository.confirmAppointment("a1") }

        vm.onOpenAppointmentActions("a1")
        vm.onRescheduleAppointment("a1", "2026-10-28", "09:30")
        settle()
        coVerify { repository.rescheduleAppointment("a1", Instant.parse("2026-10-28T14:30:00Z")) }

        vm.onOpenAppointmentActions("a1")
        vm.onCancelAppointment("a1", "No puede asistir")
        settle()
        coVerify { repository.cancelAppointment("a1", "No puede asistir") }
    }

    @Test
    fun `editar contacto actualiza el cliente seleccionado`() = runTest(mainRule.dispatcher) {
        val updated = ana.copy(email = "nuevo@correo.pe", phone = "911000222")
        coEvery { repository.getCustomerVehicles("c1") } returns Result.success(listOf(yaris))
        coEvery { repository.updateCustomerContact("c1", "nuevo@correo.pe", "911000222") } returns Result.success(updated)

        val vm = viewModel()
        settle()

        vm.onSelectCustomer(ana)
        settle()
        vm.onOpenEditContact(ana)
        vm.onEditContactEmailChange("nuevo@correo.pe")
        vm.onEditContactPhoneChange("911000222")
        vm.onConfirmEditContact()
        settle()

        assertNull(vm.uiState.value.editContactForm)
        assertEquals("nuevo@correo.pe", vm.uiState.value.selectedCustomer?.email)
        assertNotNull(vm.uiState.value.notice)
    }
}
