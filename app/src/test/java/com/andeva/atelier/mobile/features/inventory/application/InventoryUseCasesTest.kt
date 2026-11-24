package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.BatchRestoreResult
import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLineInput
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.domain.StockAllocation
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InventoryUseCasesTest {

    private val repository = mockk<InventoryRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidInventoryInputException).field

    private fun part(stock: String, minimum: String = "5", status: PartStatus? = PartStatus.ACTIVE) =
        Part("p1", "Pastillas", "BRK-001", null, null, BigDecimal(stock), BigDecimal(minimum), "UNIT", status)

    @Test
    fun `datos invalidos no llegan al backend`() = runTest {
        val receive = ReceivePurchaseOrderUseCase(repository, clock)

        assertEquals("sku", field(FindPartBySkuUseCase(repository)("ab")))
        assertEquals("sku", field(FindPartBySkuUseCase(repository)("BRK 001")))
        assertEquals("photo", field(receive("po1", "F001-1", photo = null)))
        assertEquals("receiptNumber", field(receive("po1", " ", File("x.jpg"))))
        assertEquals("receivedAt", field(receive("po1", "F001-1", File("x.jpg"), Instant.parse("2026-10-25T00:00:00Z"))))
        coVerify(exactly = 0) { repository.findBySku(any()) }
        coVerify(exactly = 0) { repository.receivePurchaseOrder(any(), any(), any(), any()) }
    }

    @Test
    fun `agotado, stock bajo y seleccionable`() {
        assertTrue(part("0").isOutOfStock)
        assertFalse(part("0").isSelectable)
        assertTrue(part("5").isLowStock)
        assertFalse(part("6").isLowStock)
        assertFalse(part("3", status = PartStatus.DISCONTINUED).isSelectable)
        assertTrue(part("3").isSelectable)
    }

    @Test
    fun `las alertas observadas se derivan de los repuestos con stock bajo`() = runTest {
        every { repository.observeLowStock() } returns flowOf(listOf(part("2")))

        val alert = ObserveStockAlertsUseCase(repository)().first().single()

        assertEquals(0, BigDecimal(3).compareTo(alert.deficit))
    }

    @Test
    fun `validaciones de repuestos, proveedores, ordenes y lotes bloquean entradas invalidas`() = runTest {
        assertEquals("name", field(CreatePartUseCase(repository)("", "BRK-001", ItemCategory.BRAKES, BigDecimal.ONE, BigDecimal.ZERO)))
        assertEquals("sku", field(CreatePartUseCase(repository)("Filtro", "x", ItemCategory.FILTERS, BigDecimal.ONE, BigDecimal.ZERO)))
        assertEquals("basePrice", field(CreatePartUseCase(repository)("Filtro", "FLT-001", ItemCategory.FILTERS, BigDecimal("-1"), BigDecimal.ZERO)))
        assertEquals("minimumStock", field(UpdatePartUseCase(repository)("p1", "Filtro", ItemCategory.FILTERS, BigDecimal.TEN, BigDecimal("-1"))))

        assertEquals("businessName", field(CreateSupplierUseCase(repository)(" ", "20512345678")))
        assertEquals("taxId", field(CreateSupplierUseCase(repository)("Pacífico SAC", "123")))
        assertEquals("email", field(CreateSupplierUseCase(repository)("Pacífico SAC", "20512345678", email = "invalido")))
        assertEquals("email", field(UpdateSupplierUseCase(repository)("s1", email = "malo")))

        assertEquals("supplierId", field(CreatePurchaseOrderUseCase(repository)("")))
        assertEquals("quantity", field(CreatePurchaseOrderUseCase(repository)("s1", listOf(PurchaseOrderLineInput("p1", BigDecimal.ZERO, BigDecimal.TEN)))))
        assertEquals("unitCost", field(AddPurchaseOrderItemUseCase(repository)("po1", "p1", BigDecimal.ONE, BigDecimal("-0.5"))))
        assertEquals("id", field(IssuePurchaseOrderUseCase(repository)(" ")))
        assertEquals("id", field(CancelPurchaseOrderUseCase(repository)("")))

        assertEquals("batchNumber", field(ReceiveDirectBatchUseCase(repository, clock)("p1", " ", BigDecimal.ONE, BigDecimal.TEN)))
        assertEquals("quantity", field(ReceiveDirectBatchUseCase(repository, clock)("p1", "L-1", BigDecimal.ZERO, BigDecimal.TEN)))
        assertEquals("arrivalDate", field(ReceiveDirectBatchUseCase(repository, clock)("p1", "L-1", BigDecimal.ONE, BigDecimal.TEN, Instant.parse("2026-10-25T00:00:00Z"))))
        assertEquals("quantity", field(DispatchFifoBatchUseCase(repository)("p1", BigDecimal("0.001"))))
        assertEquals("quantity", field(RestoreBatchUseCase(repository)("p1", BigDecimal.ZERO)))

        coVerify(exactly = 0) { repository.createPart(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createSupplier(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createPurchaseOrder(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.receiveDirectBatch(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `use cases validos delegan al repositorio`() = runTest {
        val supplier = Supplier("s1", "Pacífico SAC", "20512345678", null, null, null, null, true)
        val po = PurchaseOrder("po1", "OC-1", "s1", "b1", PurchaseOrderStatus.DRAFT, null, null, null, null, emptyList())
        val batch = Batch("b1", "p1", "L-1", BigDecimal("5"), null, null)
        val allocation = StockAllocation("a1", BigDecimal("2"), null, emptyList())
        val restored = BatchRestoreResult("OK", "Restored", "b1", BigDecimal("1"))

        coEvery { repository.getSuppliers("pac", null) } returns Result.success(listOf(supplier))
        coEvery { repository.createSupplier("Pacífico SAC", "20512345678", null, null, null, null) } returns Result.success(supplier)
        coEvery { repository.updateSupplier("s1", "Pacífico EIRL", null, null, null, null, false) } returns Result.success(supplier.copy(active = false))
        coEvery { repository.createPurchaseOrder("s1", emptyList(), null, null, null) } returns Result.success(po)
        coEvery { repository.addPurchaseOrderItem("po1", "p1", BigDecimal("3"), BigDecimal("20")) } returns Result.success(po)
        coEvery { repository.issuePurchaseOrder("po1") } returns Result.success(po.copy(status = PurchaseOrderStatus.ISSUED))
        coEvery { repository.cancelPurchaseOrder("po1", "Error") } returns Result.success(po.copy(status = PurchaseOrderStatus.CANCELED))
        coEvery { repository.receiveDirectBatch("p1", "L-1", BigDecimal("5"), BigDecimal("30"), null, null) } returns Result.success(batch)
        coEvery { repository.dispatchFifoBatch("p1", BigDecimal("2"), null, null) } returns Result.success(allocation)
        coEvery { repository.restoreBatch("p1", BigDecimal("1"), "b1", "No usado", null) } returns Result.success(restored)

        assertEquals("s1", GetSuppliersUseCase(repository)("pac").getOrThrow().single().id)
        assertEquals("s1", CreateSupplierUseCase(repository)("Pacífico SAC", "20512345678").getOrThrow().id)
        assertFalse(UpdateSupplierUseCase(repository)("s1", businessName = "Pacífico EIRL", active = false).getOrThrow().active)
        assertEquals("po1", CreatePurchaseOrderUseCase(repository)("s1").getOrThrow().id)
        assertEquals("po1", AddPurchaseOrderItemUseCase(repository)("po1", "p1", BigDecimal("3"), BigDecimal("20")).getOrThrow().id)
        assertEquals(PurchaseOrderStatus.ISSUED, IssuePurchaseOrderUseCase(repository)("po1").getOrThrow().status)
        assertEquals(PurchaseOrderStatus.CANCELED, CancelPurchaseOrderUseCase(repository)("po1", "Error").getOrThrow().status)
        assertEquals("b1", ReceiveDirectBatchUseCase(repository, clock)("p1", "L-1", BigDecimal("5"), BigDecimal("30")).getOrThrow().id)
        assertEquals("a1", DispatchFifoBatchUseCase(repository)("p1", BigDecimal("2")).getOrThrow().allocationId)
        assertEquals("b1", RestoreBatchUseCase(repository)("p1", BigDecimal("1"), "b1", "No usado").getOrThrow().batchId)
    }
}
