package com.andeva.atelier.mobile.features.inventory.presentation.orders

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.inventory.application.AddPurchaseOrderItemUseCase
import com.andeva.atelier.mobile.features.inventory.application.CancelPurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.GetPurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.IssuePurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.ReceivePurchaseOrderUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLine
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PurchaseOrderDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<InventoryRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }
    private val clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)

    private val part = Part(
        id = "p1", name = "Filtro", sku = "FLT-001", category = ItemCategory.FILTERS,
        basePrice = Money(BigDecimal("25.00"), Money.PEN), totalStock = BigDecimal("5"),
        minimumStock = BigDecimal("2"), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
    )

    private fun viewModel(): PurchaseOrderDetailViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(
            Session(
                userId = "u", email = "a@taller.pe", fullName = "Almacén", tenantId = "t",
                tenantName = "Taller", membershipId = "m",
                permissions = listOf(
                    Permission.PURCHASE_ORDERS_READ,
                    Permission.PURCHASE_ORDERS_CREATE,
                    Permission.BATCHES_RECEIVE
                ).map { it.value }.toSet(),
                plan = null
            )
        )
        every { repository.observeCatalog(null, null) } returns flowOf(listOf(part))
        every { repository.observePart("p1") } returns flowOf(part)
        return PurchaseOrderDetailViewModel(
            observeSession,
            GetPurchaseOrderUseCase(repository),
            ObservePartUseCase(repository),
            ReceivePurchaseOrderUseCase(repository, clock),
            connectivity,
            SavedStateHandle(mapOf("purchaseOrderId" to "po1")),
            ObservePartsCatalogUseCase(repository),
            RefreshPartsCatalogUseCase(repository),
            AddPurchaseOrderItemUseCase(repository),
            IssuePurchaseOrderUseCase(repository),
            CancelPurchaseOrderUseCase(repository)
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `flujo completo agregar linea emitir y anular orden de compra`() = runTest(mainRule.dispatcher) {
        val draft = PurchaseOrder("po1", "OC-1", "s1", "b1", PurchaseOrderStatus.DRAFT, null, null, null, null, emptyList())
        val withLine = draft.copy(lines = listOf(PurchaseOrderLine("l1", "p1", BigDecimal("4"), Money(BigDecimal("20.00"), Money.PEN), Money(BigDecimal("80.00"), Money.PEN))))
        val issued = withLine.copy(status = PurchaseOrderStatus.ISSUED)
        val canceled = issued.copy(status = PurchaseOrderStatus.CANCELED)

        coEvery { repository.getPurchaseOrder("po1") } returns Result.success(draft)
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        coEvery { repository.addPurchaseOrderItem("po1", "p1", BigDecimal("4"), BigDecimal("20.00")) } returns Result.success(withLine)
        coEvery { repository.issuePurchaseOrder("po1") } returns Result.success(issued)
        coEvery { repository.cancelPurchaseOrder("po1", "Cambio de proveedor") } returns Result.success(canceled)

        val vm = viewModel()
        settle()
        assertEquals(PurchaseOrderStatus.DRAFT, vm.uiState.value.order?.status)

        vm.onOpenAddLine()
        settle()
        vm.onLineQuantityChange("4")
        vm.onLineUnitCostChange("20.00")
        vm.onConfirmAddLine()
        settle()
        assertFalse(vm.uiState.value.showAddLineSheet)
        assertEquals(1, vm.uiState.value.order?.lines?.size)

        vm.onIssueOrder()
        settle()
        assertEquals(PurchaseOrderStatus.ISSUED, vm.uiState.value.order?.status)

        vm.onOpenCancel()
        vm.onCancelReasonChange("Cambio de proveedor")
        vm.onConfirmCancel()
        settle()
        assertFalse(vm.uiState.value.showCancelSheet)
        assertEquals(PurchaseOrderStatus.CANCELED, vm.uiState.value.order?.status)
    }
}
