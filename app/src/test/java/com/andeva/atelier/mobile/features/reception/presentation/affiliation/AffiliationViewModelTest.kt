package com.andeva.atelier.mobile.features.reception.presentation.affiliation

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.AffiliateCustomerUseCase
import com.andeva.atelier.mobile.features.reception.application.LookupAffiliationUseCase
import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.AffiliationStep
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.PlatformAccount
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
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

class AffiliationViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<ReceptionRepository>()
    private val ana = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", "ana@correo.pe", null, "ACTIVE")
    private val account = PlatformAccount("u1", "ana@correo.pe", "Ana Torres", "ACTIVE")

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "r@taller.pe", fullName = "Recepción", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(session: Session? = sessionWith(listOf(Permission.CUSTOMERS_READ, Permission.CUSTOMERS_CREATE))): AffiliationViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(session)
        return AffiliationViewModel(observeSession, LookupAffiliationUseCase(repository), AffiliateCustomerUseCase(repository))
    }

    private fun AffiliationViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun lookup(step: AffiliationStep?, customer: Customer? = null) =
        AffiliationLookup(workshopCustomer = customer, platformAccount = account, vehicles = emptyList(), nextStep = step)

    @Test
    fun `los permisos de la sesion habilitan buscar y afiliar`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        assertTrue(vm.uiState.value.canLookup)
        assertTrue(vm.uiState.value.canAffiliate)
    }

    @Test
    fun `sin permiso de lectura no se puede buscar`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(sessionWith(listOf(Permission.CUSTOMERS_CREATE)))
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()

        coVerify(exactly = 0) { repository.lookupAffiliation(any()) }
        assertFalse(vm.uiState.value.canSearch)
    }

    @Test
    fun `buscar un correo con cuenta en Atelier pide afiliar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation("ana@correo.pe") } returns Result.success(lookup(AffiliationStep.AFFILIATE))
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange(" ana@correo.pe ")
        vm.onSearch()
        vm.settle()

        assertEquals(AffiliationStep.AFFILIATE, vm.uiState.value.step)
        assertFalse(vm.uiState.value.isSearching)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `un correo invalido marca el campo y no llama al backend`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@")
        vm.onSearch()
        vm.settle()

        coVerify(exactly = 0) { repository.lookupAffiliation(any()) }
        assertEquals("email", vm.uiState.value.invalidField)
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `afiliar envia documento y telefono y deja al cliente afiliado`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.success(lookup(AffiliationStep.AFFILIATE))
        coEvery { repository.affiliate("ana@correo.pe", "45678912", "999111222") } returns Result.success(ana)
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()
        vm.onTaxIdChange(" 45678912 ")
        vm.onPhoneChange("999111222")
        vm.onAffiliate()
        vm.settle()

        assertEquals(ana, vm.uiState.value.affiliated)
        assertFalse(vm.uiState.value.isAffiliating)
        assertFalse(vm.uiState.value.canSubmitAffiliation)
    }

    @Test
    fun `el telefono vacio se envia como null`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.success(lookup(AffiliationStep.AFFILIATE))
        coEvery { repository.affiliate("ana@correo.pe", "45678912", null) } returns Result.success(ana)
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()
        vm.onTaxIdChange("45678912")
        vm.onAffiliate()
        vm.settle()

        coVerify(exactly = 1) { repository.affiliate("ana@correo.pe", "45678912", null) }
    }

    @Test
    fun `sin documento no se puede afiliar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.success(lookup(AffiliationStep.AFFILIATE))
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()
        vm.onAffiliate()
        vm.settle()

        coVerify(exactly = 0) { repository.affiliate(any(), any(), any()) }
    }

    @Test
    fun `un cliente que ya esta en el taller no se afilia otra vez`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.success(lookup(AffiliationStep.ALREADY_CUSTOMER, ana))
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()
        vm.onTaxIdChange("45678912")
        vm.onAffiliate()
        vm.settle()

        assertEquals(AffiliationStep.ALREADY_CUSTOMER, vm.uiState.value.step)
        coVerify(exactly = 0) { repository.affiliate(any(), any(), any()) }
    }

    @Test
    fun `un error del backend muestra su mensaje`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.failure(ApiException(500, "ERR", "Servicio no disponible."))
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()

        assertEquals("Servicio no disponible.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `sin red se muestra el aviso de conexion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()

        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `cambiar el correo descarta el resultado anterior`() = runTest(mainRule.dispatcher) {
        coEvery { repository.lookupAffiliation(any()) } returns Result.success(lookup(AffiliationStep.REGISTER_WALK_IN))
        val vm = viewModel()
        vm.settle()
        vm.onEmailChange("ana@correo.pe")
        vm.onSearch()
        vm.settle()
        assertEquals(AffiliationStep.REGISTER_WALK_IN, vm.uiState.value.step)

        vm.onEmailChange("luis@correo.pe")
        assertNull(vm.uiState.value.lookup)
    }
}
