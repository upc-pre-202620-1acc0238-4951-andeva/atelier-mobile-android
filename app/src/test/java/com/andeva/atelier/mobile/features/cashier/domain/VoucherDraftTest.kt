package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VoucherDraftTest {

    private fun pen(value: String) = Money(BigDecimal(value), Money.PEN)

    private fun product(name: String?, quantity: String, price: String?) = TaskProduct(
        id = "tp-$name", productId = "prod-1", productName = name,
        quantity = BigDecimal(quantity), unitPrice = price?.let { pen(it) }, totalAmount = null
    )

    private fun task(
        price: String?,
        status: TaskStatus? = TaskStatus.COMPLETED,
        serviceId: String? = "svc-1",
        products: List<TaskProduct> = emptyList()
    ) = TaskDetail(
        id = TaskId("t-1"), workOrderId = "wo-1", serviceId = serviceId, serviceName = "Standard Service",
        mechanicId = null, mechanicName = null, status = status, description = "Cambio de aceite",
        price = price?.let { pen(it) }, holdReason = null, missingItemDescription = null, totalPausedSeconds = 0,
        startedAt = null, completedAt = null, pendingSuspensionReason = null, pendingSuspensionRequestedAt = null,
        products = products, evidenceImages = emptyList()
    )

    private fun detail(vararg tasks: TaskDetail) = WorkOrderDetail(
        workOrder = WorkOrder(
            id = "wo-1", internalNumber = 1, branchId = "b-1", vehicleId = "v-1", customerId = "c-1",
            currentBayId = null, bayName = null, status = null, diagnosticSummary = null, mileageIn = null,
            subtotal = null, taxAmount = null, totalAmount = pen("118.00")
        ),
        tasks = tasks.toList(), proposals = emptyList(), intakeImages = emptyList(), createdAt = null, updatedAt = null
    )

    private fun customer(type: CustomerType?, taxId: String?) = Customer(
        id = "c-1", type = type, displayName = "Cliente SA", taxId = taxId, email = null, phone = null, status = null
    )

    @Test
    fun `arma una linea de servicio y una de repuesto con IGV`() {
        val lines = VoucherDrafts.fromWorkOrder(
            detail(task("100.00", products = listOf(product("Filtro", "2", "25.00")))),
            serviceNames = emptyMap(), partNames = emptyMap()
        )

        assertEquals(2, lines.size)
        assertEquals(VoucherItemType.SERVICE, lines[0].itemType)
        assertEquals("svc-1", lines[0].itemId)
        assertEquals(BigDecimal.ONE, lines[0].quantity)
        assertEquals(BigDecimal("118.00"), lines[0].unitPriceWithIgv)
        assertEquals(VoucherItemType.PRODUCT, lines[1].itemType)
        assertEquals("prod-1", lines[1].itemId)
        assertEquals(BigDecimal("2"), lines[1].quantity)
        assertEquals(BigDecimal("29.50"), lines[1].unitPriceWithIgv)
    }

    @Test
    fun `omite las tareas canceladas`() {
        val lines = VoucherDrafts.fromWorkOrder(
            detail(
                task("100.00"),
                task("50.00", status = TaskStatus.CANCELLED, products = listOf(product("Filtro", "1", "10.00")))
            ),
            serviceNames = emptyMap(), partNames = emptyMap()
        )

        assertEquals(1, lines.size)
        assertEquals(BigDecimal("118.00"), lines[0].unitPriceWithIgv)
    }

    @Test
    fun `usa el nombre del catalogo y no Standard Service`() {
        val lines = VoucherDrafts.fromWorkOrder(
            detail(task("100.00", products = listOf(product("Nombre falso", "1", "10.00")))),
            serviceNames = mapOf("svc-1" to "Cambio de aceite sintetico"),
            partNames = mapOf("prod-1" to "Filtro de aceite")
        )

        assertEquals("Cambio de aceite sintetico", lines[0].description)
        assertEquals("Filtro de aceite", lines[1].description)
    }

    @Test
    fun `sin catalogo usa la descripcion de la tarea y el nombre del repuesto`() {
        val lines = VoucherDrafts.fromWorkOrder(
            detail(task("100.00", products = listOf(product("Filtro", "1", "10.00")))),
            serviceNames = emptyMap(), partNames = emptyMap()
        )

        assertEquals("Cambio de aceite", lines[0].description)
        assertEquals("Filtro", lines[1].description)
    }

    @Test
    fun `una tarea en 0 no da linea pero sus repuestos si`() {
        val lines = VoucherDrafts.fromWorkOrder(
            detail(task("0.00", products = listOf(product("Filtro", "1", "10.00")))),
            serviceNames = emptyMap(), partNames = emptyMap()
        )

        assertEquals(listOf(VoucherItemType.PRODUCT), lines.map { it.itemType })
        assertEquals(BigDecimal("11.80"), lines.single().unitPriceWithIgv)
    }

    @Test
    fun `un repuesto en 0 no da linea`() {
        val lines = VoucherDrafts.fromWorkOrder(
            detail(task("100.00", products = listOf(product("Arandela", "4", "0")))),
            serviceNames = emptyMap(), partNames = emptyMap()
        )

        assertEquals(listOf(VoucherItemType.SERVICE), lines.map { it.itemType })
    }

    @Test
    fun `precio nulo o sin lineas que cobrar falla con el campo lines`() {
        listOf(
            detail(task(null)),
            detail(task("100.00", products = listOf(product("Filtro", "1", null)))),
            detail(task("0.00", products = listOf(product("Filtro", "1", "0.00")))),
            detail(task("100.00", status = TaskStatus.CANCELLED)),
            detail()
        ).forEach { d ->
            try {
                VoucherDrafts.fromWorkOrder(d, emptyMap(), emptyMap())
                fail("Debia fallar")
            } catch (e: InvalidCashierInputException) {
                assertEquals("lines", e.field)
            }
        }
    }

    @Test
    fun `las monedas salen solo de lo que da lineas`() {
        val usd = Money(BigDecimal("10.00"), Money.USD)
        val cancelled = task("50.00", status = TaskStatus.CANCELLED).copy(price = usd)
        val freePart = product("Arandela", "1", null).copy(unitPrice = Money(BigDecimal.ZERO, Money.USD))

        assertEquals(
            setOf(Money.PEN),
            VoucherDrafts.currenciesOf(detail(task("100.00", products = listOf(freePart)), cancelled))
        )
    }

    @Test
    fun `una linea sin descripcion o con el nombre generico del detalle pide editarla`() {
        fun line(type: VoucherItemType, description: String) =
            VoucherLineDraft(type, "id", description, BigDecimal.ONE, BigDecimal("1.18"))

        assertTrue(line(VoucherItemType.SERVICE, "  ").needsDescription)
        assertTrue(line(VoucherItemType.SERVICE, "Standard Service").needsDescription)
        assertTrue(line(VoucherItemType.PRODUCT, "Product 3f2b8c1e-9d4a-4b7e-8a61-2c5d9e0f1a7b").needsDescription)
        assertFalse(line(VoucherItemType.SERVICE, "Cambio de aceite").needsDescription)
        assertFalse(line(VoucherItemType.PRODUCT, "Product Filtro K&N").needsDescription)
    }

    @Test
    fun `empresa con RUC sugiere factura y persona con DNI sugiere boleta`() {
        val company = VoucherDrafts.suggestFiscalInfo(customer(CustomerType.COMPANY, "20123456789"))
        assertEquals(VoucherType.FACTURA, company.first)
        assertEquals(FiscalDocumentType.RUC, company.second.documentType)
        assertEquals("20123456789", company.second.taxId)
        assertEquals("Cliente SA", company.second.legalName)
        assertNull(company.second.fiscalAddress)

        val person = VoucherDrafts.suggestFiscalInfo(customer(CustomerType.INDIVIDUAL, "12345678"))
        assertEquals(VoucherType.BOLETA, person.first)
        assertEquals(FiscalDocumentType.DNI, person.second.documentType)
    }

    @Test
    fun `lo demas sugiere boleta con CE para que la UI lo corrija`() {
        val noRuc = VoucherDrafts.suggestFiscalInfo(customer(CustomerType.COMPANY, "123"))
        assertEquals(VoucherType.BOLETA, noRuc.first)
        assertEquals(FiscalDocumentType.CE, noRuc.second.documentType)
        assertEquals("123", noRuc.second.taxId)

        val noTaxId = VoucherDrafts.suggestFiscalInfo(customer(null, null))
        assertEquals(VoucherType.BOLETA, noTaxId.first)
        assertEquals(FiscalDocumentType.CE, noTaxId.second.documentType)
        assertEquals("", noTaxId.second.taxId)
    }

    @Test
    fun `el total del borrador puede diferir en centimos del total de la orden`() {
        // Riesgo 3: tres lineas de 33.33 pasan a IGV linea por linea (39.33 cada una) y suman 117.99, mientras la
        // orden dice 118.00. El saldo a cobrar sale del comprobante emitido, no de la orden.
        val detail = detail(task("33.33"), task("33.33"), task("33.33"))
        val draft = VoucherDraft(
            workOrderId = "wo-1", customerId = "c-1", type = VoucherType.BOLETA,
            customerInfo = CustomerFiscalInfo(FiscalDocumentType.DNI, "12345678", "Cliente", null),
            currency = Money.PEN, lines = VoucherDrafts.fromWorkOrder(detail, emptyMap(), emptyMap())
        )

        assertEquals(BigDecimal("117.99"), draft.total)
        assertEquals(BigDecimal("118.00"), detail.workOrder.totalAmount?.amount)
    }
}
