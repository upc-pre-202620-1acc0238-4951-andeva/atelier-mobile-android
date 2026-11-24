package com.andeva.atelier.mobile.features.inventory.presentation.scanner

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.inventory.application.FindPartBySkuUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class SkuScannerViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<InventoryRepository>()
    private val part = Part(
        id = "p1", name = "Pastillas", sku = "BRK-001", category = ItemCategory.BRAKES,
        basePrice = Money(BigDecimal("80.00"), Money.PEN), totalStock = BigDecimal("10"),
        minimumStock = BigDecimal("2"), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
    )

    private fun viewModel(permissions: List<Permission> = listOf(Permission.PARTS_READ)): SkuScannerViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(
            Session(
                userId = "u", email = "a@taller.pe", fullName = "Almacén", tenantId = "t",
                tenantName = "Taller", membershipId = "m", permissions = permissions.map { it.value }.toSet(),
                plan = null
            )
        )
        return SkuScannerViewModel(observeSession, FindPartBySkuUseCase(repository))
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `detectar codigo valido encuentra el repuesto y evita llamadas duplicadas`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findBySku("brk-001") } returns Result.success(part)
        val vm = viewModel()
        settle()

        vm.onBarcodeDetected("brk-001")
        vm.onBarcodeDetected("BRK-001")
        settle()

        assertEquals("p1", vm.uiState.value.foundPartId)
        coVerify(exactly = 1) { repository.findBySku("brk-001") }

        vm.onPartNavigated()
        assertNull(vm.uiState.value.foundPartId)
    }

    @Test
    fun `sku no encontrado muestra aviso y permite reintentar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.findBySku("XXX-999") } returns Result.success(null)
        val vm = viewModel()
        settle()

        vm.onManualSkuChange("XXX-999")
        vm.onSubmitManualSku()
        settle()

        assertEquals("XXX-999", vm.uiState.value.notFoundSku)
        vm.onResetScan()
        assertNull(vm.uiState.value.notFoundSku)
    }

    @Test
    fun `codigo con formato invalido muestra invalidSku sin llamar al backend`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        settle()

        vm.onBarcodeDetected("ab")
        settle()

        assertEquals("ab", vm.uiState.value.invalidSku)
        coVerify(exactly = 0) { repository.findBySku(any()) }
    }

    @Test
    fun `sin permiso parts read bloquea el escaner`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(emptyList())
        settle()

        assertFalse(vm.uiState.value.canRead)
        vm.onBarcodeDetected("BRK-001")
        settle()
        coVerify(exactly = 0) { repository.findBySku(any()) }
    }
}
