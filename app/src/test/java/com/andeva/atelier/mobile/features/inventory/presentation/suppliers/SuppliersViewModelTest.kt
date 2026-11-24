package com.andeva.atelier.mobile.features.inventory.presentation.suppliers

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.inventory.application.CreateSupplierUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetSuppliersUseCase
import com.andeva.atelier.mobile.features.inventory.application.UpdateSupplierUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SuppliersViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<InventoryRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private val supplier = Supplier("s1", "Pacífico SAC", "20512345678", "Carlos", "998112233", "ventas@pacifico.pe", "Lima", true)

    private fun viewModel(permissions: List<Permission> = listOf(Permission.SUPPLIERS_MANAGE)): SuppliersViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(
            Session(
                userId = "u", email = "a@taller.pe", fullName = "Almacén", tenantId = "t",
                tenantName = "Taller", membershipId = "m", permissions = permissions.map { it.value }.toSet(),
                plan = null
            )
        )
        return SuppliersViewModel(
            observeSession,
            GetSuppliersUseCase(repository),
            CreateSupplierUseCase(repository),
            UpdateSupplierUseCase(repository),
            connectivity
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `carga proveedores y valida campos al crear y editar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getSuppliers(null, null) } returns Result.success(listOf(supplier))
        coEvery {
            repository.createSupplier("Motor Sur SAC", "20601122334", "", "", "", "")
        } returns Result.success(supplier.copy(id = "s2", businessName = "Motor Sur SAC", taxId = "20601122334"))
        coEvery {
            repository.updateSupplier("s1", "Pacífico Editado", "Carlos", "998112233", "ventas@pacifico.pe", "Lima", false)
        } returns Result.success(supplier.copy(businessName = "Pacífico Editado", active = false))

        val vm = viewModel()
        settle()
        assertEquals(1, vm.uiState.value.suppliers.size)

        vm.onOpenCreate()
        vm.onBusinessNameChange(" ")
        vm.onTaxIdChange("123")
        vm.onEmailChange("correo-malo")
        vm.onSave()
        settle()

        assertTrue(vm.uiState.value.nameError != null)
        assertTrue(vm.uiState.value.taxIdError != null)
        assertTrue(vm.uiState.value.emailError != null)
        coVerify(exactly = 0) { repository.createSupplier(any(), any(), any(), any(), any(), any()) }

        vm.onBusinessNameChange("Motor Sur SAC")
        vm.onTaxIdChange("20601122334")
        vm.onEmailChange("")
        vm.onSave()
        settle()
        assertFalse(vm.uiState.value.sheetOpen)

        vm.onOpenEdit(supplier)
        vm.onBusinessNameChange("Pacífico Editado")
        vm.onActiveChange(false)
        vm.onSave()
        settle()
        assertFalse(vm.uiState.value.sheetOpen)
    }

    @Test
    fun `sin permiso suppliers manage deshabilita gestion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getSuppliers(any(), any()) } returns Result.success(emptyList())
        val vm = viewModel(emptyList())
        settle()
        assertFalse(vm.uiState.value.canManage)
    }
}
