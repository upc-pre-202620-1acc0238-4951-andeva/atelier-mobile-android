package com.andeva.atelier.mobile.features.inventory.presentation.orders

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.inventory.application.CreatePurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrdersUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetSuppliersUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
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

class PurchaseOrdersViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<InventoryRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private fun viewModel(
        permissions: List<Permission> = listOf(Permission.PURCHASE_ORDERS_READ, Permission.PURCHASE_ORDERS_CREATE)
    ): PurchaseOrdersViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(
            Session(
                userId = "u", email = "a@taller.pe", fullName = "Almacén", tenantId = "t",
                tenantName = "Taller", membershipId = "m", permissions = permissions.map { it.value }.toSet(),
                plan = null, branchId = "b1"
            )
        )
        return PurchaseOrdersViewModel(
            observeSession,
            GetPurchaseOrdersUseCase(repository),
            GetSuppliersUseCase(repository),
            CreatePurchaseOrderUseCase(repository),
            connectivity
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `crea orden en borrador eligiendo proveedor y emite navegacion`() = runTest(mainRule.dispatcher) {
        val supplier = Supplier("s1", "Pacífico SAC", "20512345678", null, null, null, null, true)
        val draft = PurchaseOrder("po9", "OC-99", "s1", "b1", PurchaseOrderStatus.DRAFT, null, null, null, null, emptyList())
        coEvery { repository.getPurchaseOrders(PurchaseOrderStatus.ISSUED) } returns Result.success(emptyList())
        coEvery { repository.getSuppliers(null, null) } returns Result.success(listOf(supplier))
        coEvery { repository.createPurchaseOrder("s1", emptyList(), "", null, "") } returns Result.success(draft)

        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.canCreate)

        vm.onOpenCreateSheet()
        settle()
        assertEquals("s1", vm.uiState.value.selectedSupplierId)

        vm.onCreateOrder()
        settle()

        assertEquals("po9", vm.uiState.value.createdOrderId)
        assertFalse(vm.uiState.value.createSheetOpen)
        vm.onCreatedOrderNavigated()
        assertNull(vm.uiState.value.createdOrderId)
    }

    @Test
    fun `filtra por stock minimo y genera borrador automatico con todos los repuestos bajos`() = runTest(mainRule.dispatcher) {
        val supplier = Supplier("s1", "Repuestos Sur", "20512345678", null, null, null, null, true)
        val okPart = com.andeva.atelier.mobile.features.inventory.domain.Part(
            id = "p-ok", name = "Aceite 5W-30", sku = "OIL-1", category = null,
            basePrice = com.andeva.atelier.mobile.core.domain.Money(java.math.BigDecimal("45.00"), "PEN"),
            totalStock = java.math.BigDecimal("20"), minimumStock = java.math.BigDecimal("5"),
            unitOfMeasure = "UNIT", status = com.andeva.atelier.mobile.features.inventory.domain.PartStatus.ACTIVE
        )
        val lowPart = com.andeva.atelier.mobile.features.inventory.domain.Part(
            id = "p-low", name = "Batería 12V", sku = "BAT-1", category = null,
            basePrice = com.andeva.atelier.mobile.core.domain.Money(java.math.BigDecimal("300.00"), "PEN"),
            totalStock = java.math.BigDecimal("2"), minimumStock = java.math.BigDecimal("4"),
            unitOfMeasure = "UNIT", status = com.andeva.atelier.mobile.features.inventory.domain.PartStatus.ACTIVE
        )
        val outPart = com.andeva.atelier.mobile.features.inventory.domain.Part(
            id = "p-out", name = "Filtro aire", sku = "FLT-1", category = null,
            basePrice = com.andeva.atelier.mobile.core.domain.Money(java.math.BigDecimal("25.00"), "PEN"),
            totalStock = java.math.BigDecimal("0"), minimumStock = java.math.BigDecimal("3"),
            unitOfMeasure = "UNIT", status = com.andeva.atelier.mobile.features.inventory.domain.PartStatus.ACTIVE
        )
        val draft = PurchaseOrder("po-low", "OC-100", "s1", "b1", PurchaseOrderStatus.DRAFT, null, null, null, null, emptyList())

        val observeCatalog = mockk<com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase>()
        every { observeCatalog.invoke(null) } returns flowOf(listOf(okPart, lowPart, outPart))
        coEvery { repository.getPurchaseOrders(PurchaseOrderStatus.ISSUED) } returns Result.success(emptyList())
        coEvery { repository.getSuppliers(null, null) } returns Result.success(listOf(supplier))
        coEvery {
            repository.createPurchaseOrder(
                supplierId = "s1",
                items = listOf(
                    com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLineInput("p-out", java.math.BigDecimal("6"), java.math.BigDecimal("25.00")),
                    com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLineInput("p-low", java.math.BigDecimal("6"), java.math.BigDecimal("300.00"))
                ),
                orderNumber = "",
                expectedDeliveryDate = null,
                notes = ""
            )
        } returns Result.success(draft)

        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(
            Session(
                userId = "u", email = "a@taller.pe", fullName = "Almacén", tenantId = "t",
                tenantName = "Taller", membershipId = "m",
                permissions = setOf(Permission.PURCHASE_ORDERS_READ.value, Permission.PURCHASE_ORDERS_CREATE.value),
                plan = null, branchId = "b1"
            )
        )
        val vm = PurchaseOrdersViewModel(
            observeSession,
            GetPurchaseOrdersUseCase(repository),
            GetSuppliersUseCase(repository),
            CreatePurchaseOrderUseCase(repository),
            connectivity,
            observeCatalog = observeCatalog
        )
        settle()

        // Low-stock parts are sorted first (out-of-stock -> low-stock -> normal)
        assertEquals(listOf("p-out", "p-low", "p-ok"), vm.uiState.value.filteredCatalogParts.map { it.id })

        vm.onOpenCreateSheet()
        settle()
        vm.onToggleLowStockOnly()
        assertTrue(vm.uiState.value.lowStockOnly)
        assertEquals(listOf("p-out", "p-low"), vm.uiState.value.filteredCatalogParts.map { it.id })

        vm.onCreateLowStockOrder()
        settle()
        assertEquals("po-low", vm.uiState.value.createdOrderId)
        assertFalse(vm.uiState.value.createSheetOpen)
    }
}
