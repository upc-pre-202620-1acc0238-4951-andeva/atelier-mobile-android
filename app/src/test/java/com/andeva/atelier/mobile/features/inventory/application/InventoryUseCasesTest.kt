package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
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
}
