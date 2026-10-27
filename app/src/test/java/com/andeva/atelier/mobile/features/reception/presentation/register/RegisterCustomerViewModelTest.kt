package com.andeva.atelier.mobile.features.reception.presentation.register

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.RegisterCompanyCustomerUseCase
import com.andeva.atelier.mobile.features.reception.application.RegisterIndividualCustomerUseCase
import com.andeva.atelier.mobile.features.reception.application.RegisterVehicleUseCase
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.NewCompanyCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewIndividualCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewVehicle
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
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

class RegisterCustomerViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<ReceptionRepository>()
    private val ana = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", null, null, "ACTIVE")
    private val empresa = Customer("c2", CustomerType.COMPANY, "Transportes Andes SAC", "20123456789", null, null, "ACTIVE")
    private val yaris = Vehicle("v1", "ABC123", null, "Toyota", "Yaris", null, EngineType.GASOLINE, "c1", null)

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "r@taller.pe", fullName = "Recepción", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(
        session: Session = sessionWith(listOf(Permission.CUSTOMERS_CREATE, Permission.VEHICLES_CREATE)),
        email: String? = null
    ): RegisterCustomerViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(session)
        return RegisterCustomerViewModel(
            observeSession,
            RegisterIndividualCustomerUseCase(repository),
            RegisterCompanyCustomerUseCase(repository),
            RegisterVehicleUseCase(repository),
            SavedStateHandle(if (email == null) emptyMap() else mapOf("email" to email))
        )
    }

    private fun RegisterCustomerViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun RegisterCustomerViewModel.fillIndividual() {
        onFirstNameChange("Ana")
        onLastNameChange("Torres")
        onTaxIdChange("45678912")
        onPlateChange("abc 123")
        onBrandChange("Toyota")
        onModelChange("Yaris")
    }

    @Test
    fun `el correo de la afiliacion llega precargado`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(email = "ana@correo.pe")
        vm.settle()
        assertEquals("ana@correo.pe", vm.uiState.value.email)
    }

    @Test
    fun `se habilita con los permisos de crear cliente y vehiculo`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        assertTrue(vm.uiState.value.canRegister)

        val sinVehiculo = viewModel(sessionWith(listOf(Permission.CUSTOMERS_CREATE)))
        sinVehiculo.settle()
        assertFalse(sinVehiculo.uiState.value.canRegister)
    }

    @Test
    fun `registra a la persona y luego el vehiculo con su titular`() = runTest(mainRule.dispatcher) {
        coEvery { repository.registerIndividual(NewIndividualCustomer("Ana", "Torres", "45678912")) } returns Result.success(ana)
        coEvery { repository.registerVehicle(any()) } returns Result.success(yaris)
        val vm = viewModel()
        vm.settle()
        vm.fillIndividual()
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 1) {
            repository.registerVehicle(NewVehicle("abc 123", "Toyota", "Yaris", EngineType.GASOLINE, "c1", null, null))
        }
        assertTrue(vm.uiState.value.done)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `registra una empresa con su RUC`() = runTest(mainRule.dispatcher) {
        coEvery { repository.registerCompany(NewCompanyCustomer("Transportes Andes SAC", "20123456789")) } returns Result.success(empresa)
        coEvery { repository.registerVehicle(any()) } returns Result.success(yaris)
        val vm = viewModel()
        vm.settle()
        vm.onKindChange(CustomerType.COMPANY)
        vm.onCompanyNameChange("Transportes Andes SAC")
        vm.onTaxIdChange("20123456789")
        vm.onPlateChange("ABC123")
        vm.onBrandChange("Toyota")
        vm.onModelChange("Yaris")
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 0) { repository.registerIndividual(any()) }
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `faltan datos obligatorios y no se puede enviar`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onFirstNameChange("Ana")
        vm.onSubmit()
        vm.settle()

        assertFalse(vm.uiState.value.canSubmit)
        coVerify(exactly = 0) { repository.registerIndividual(any()) }
    }

    @Test
    fun `sin permisos no se registra nada`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(sessionWith(listOf(Permission.CUSTOMERS_READ)))
        vm.settle()
        vm.fillIndividual()
        vm.onSubmit()
        vm.settle()

        assertFalse(vm.uiState.value.canSubmit)
        coVerify(exactly = 0) { repository.registerIndividual(any()) }
    }

    @Test
    fun `un documento invalido marca el campo y no crea al cliente`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.fillIndividual()
        vm.onTaxIdChange("123")
        vm.onSubmit()
        vm.settle()

        assertEquals("taxId", vm.uiState.value.invalidField)
        assertNull(vm.uiState.value.registeredCustomerId)
        coVerify(exactly = 0) { repository.registerIndividual(any()) }
    }

    @Test
    fun `un anio invalido no crea al cliente`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.fillIndividual()
        vm.onYearChange("20x4") // solo deja los digitos: 204 es anterior a 1950
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 0) { repository.registerIndividual(any()) }
        assertEquals("year", vm.uiState.value.invalidField)
    }

    @Test
    fun `si falla el vehiculo el cliente se conserva y al reintentar no se duplica`() = runTest(mainRule.dispatcher) {
        coEvery { repository.registerIndividual(any()) } returns Result.success(ana)
        coEvery { repository.registerVehicle(any()) } returnsMany listOf(
            Result.failure(ApiException(409, "ERR", "La placa ya existe.")),
            Result.success(yaris)
        )
        val vm = viewModel()
        vm.settle()
        vm.fillIndividual()
        vm.onSubmit()
        vm.settle()

        assertEquals("c1", vm.uiState.value.registeredCustomerId)
        assertTrue(vm.uiState.value.customerLocked)
        assertEquals("La placa ya existe.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertFalse(vm.uiState.value.done)

        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 1) { repository.registerIndividual(any()) }
        coVerify(exactly = 2) { repository.registerVehicle(any()) }
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `con el cliente ya creado sus datos no se pueden editar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.registerIndividual(any()) } returns Result.success(ana)
        coEvery { repository.registerVehicle(any()) } returns Result.failure(ApiException(500, "ERR", "Error."))
        val vm = viewModel()
        vm.settle()
        vm.fillIndividual()
        vm.onSubmit()
        vm.settle()

        vm.onFirstNameChange("Otra")
        vm.onKindChange(CustomerType.COMPANY)
        vm.onModelChange("Corolla")

        assertEquals("Ana", vm.uiState.value.firstName)
        assertEquals(CustomerType.INDIVIDUAL, vm.uiState.value.kind)
        assertEquals("Corolla", vm.uiState.value.model)
    }

    @Test
    fun `el motor elegido viaja en el vehiculo`() = runTest(mainRule.dispatcher) {
        coEvery { repository.registerIndividual(any()) } returns Result.success(ana)
        coEvery { repository.registerVehicle(any()) } returns Result.success(yaris)
        val vm = viewModel()
        vm.settle()
        vm.fillIndividual()
        vm.onEngineTypeChange(EngineType.DIESEL)
        vm.onVinChange(" 1HGCM82633A004352 ")
        vm.onYearChange("2019")
        vm.onSubmit()
        vm.settle()

        coVerify {
            repository.registerVehicle(
                NewVehicle("abc 123", "Toyota", "Yaris", EngineType.DIESEL, "c1", "1HGCM82633A004352", 2019)
            )
        }
    }
}
