package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.MissingFiscalSeriesException
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLineDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class CashierUseCasesTest {

    private val cashier = mockk<CashierRepository>()
    private val workOrders = mockk<WorkOrderRepository>()
    private val inventory = mockk<InventoryRepository>()
    private val reception = mockk<ReceptionRepository>()

    private fun pen(value: String) = Money(BigDecimal(value), Money.PEN)

    private fun info(taxId: String) = CustomerFiscalInfo(FiscalDocumentType.RUC, taxId, "Taller SA", null)

    private fun draft(type: VoucherType, taxId: String) = VoucherDraft(
        workOrderId = "wo1", customerId = "c1", type = type, customerInfo = info(taxId), currency = "PEN",
        lines = listOf(VoucherLineDraft(VoucherItemType.SERVICE, "s1", "Frenos", BigDecimal.ONE, BigDecimal("118.00")))
    )

    private fun series(type: VoucherType, serie: String, active: Boolean) =
        FiscalSeries("fs-$serie", "b1", type, serie, 0, active)

    private fun voucher(paymentStatus: VoucherPaymentStatus?) = Voucher(
        id = "v1", branchId = "b1", customerId = "c1", workOrderId = "wo1", type = VoucherType.BOLETA, serie = "B001",
        number = 1, subtotal = pen("100.00"), igvAmount = pen("18.00"), totalAmount = pen("118.00"),
        paidAmount = pen("118.00"), pendingBalance = pen("0.00"), paymentStatus = paymentStatus, status = null,
        customerInfo = null, pdfUrl = null, xmlUrl = null, cdrUrl = null, sunatResponseCode = null,
        sunatDescription = null, lines = emptyList(), payments = emptyList(), issuedAt = null
    )

    private fun task(price: Money?, products: List<TaskProduct> = emptyList()) = TaskDetail(
        id = TaskId("t1"), workOrderId = "wo1", serviceId = "s1", serviceName = "Standard Service",
        mechanicId = null, mechanicName = null, status = TaskStatus.COMPLETED, description = "Cambio de aceite",
        price = price, holdReason = null, missingItemDescription = null, totalPausedSeconds = 0, startedAt = null,
        completedAt = null, pendingSuspensionReason = null, pendingSuspensionRequestedAt = null,
        products = products, evidenceImages = emptyList()
    )

    private fun product(price: Money?) = TaskProduct("tp1", "p1", "Filtro (sin nombre)", BigDecimal(2), price, null)

    private fun detail(status: WorkOrderStatus?, vararg tasks: TaskDetail) = WorkOrderDetail(
        workOrder = WorkOrder(
            id = "wo1", internalNumber = 1, branchId = "b1", vehicleId = "v1", customerId = "c1", currentBayId = null,
            bayName = null, status = status, diagnosticSummary = null, mileageIn = null, subtotal = null,
            taxAmount = null, totalAmount = pen("118.00")
        ),
        tasks = tasks.toList(), proposals = emptyList(), intakeImages = emptyList(), createdAt = null, updatedAt = null
    )

    private fun customer(type: CustomerType, taxId: String) =
        Customer("c1", type, "Taller SA", taxId, null, null, "ACTIVE")

    private fun part(name: String) = Part("p1", name, "SKU", null, null, BigDecimal.TEN, BigDecimal.ONE, "UNIT", null)

    private fun prepare() = PrepareVoucherUseCase(workOrders, inventory, reception)

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidCashierInputException).field

    private fun stubPrepare(detail: WorkOrderDetail, catalog: Result<List<ServiceOption>>) {
        coEvery { workOrders.getDetail("wo1") } returns Result.success(detail)
        coEvery { workOrders.getServiceCatalog() } returns catalog
        every { inventory.observePart("p1") } returns flowOf(part("Filtro de aceite"))
        coEvery { reception.getCustomer("c1") } returns Result.success(customer(CustomerType.COMPANY, "20123456789"))
    }

    @Test
    fun `factura sin RUC no se emite`() = runTest {
        val result = IssueVoucherUseCase(cashier)(draft(VoucherType.FACTURA, "45678912"))

        assertEquals("taxId", field(result))
        coVerify(exactly = 0) { cashier.getBranchSeries() }
        coVerify(exactly = 0) { cashier.issue(any(), any()) }
    }

    @Test
    fun `sin serie activa del tipo falla con MissingFiscalSeriesException`() = runTest {
        coEvery { cashier.getBranchSeries() } returns Result.success(
            listOf(series(VoucherType.FACTURA, "F001", active = false), series(VoucherType.BOLETA, "B001", active = true))
        )

        val result = IssueVoucherUseCase(cashier)(draft(VoucherType.FACTURA, "20123456789"))

        assertTrue(result.exceptionOrNull() is MissingFiscalSeriesException)
        coVerify(exactly = 0) { cashier.issue(any(), any()) }
    }

    @Test
    fun `emite con la primera serie activa del tipo`() = runTest {
        val issued = IssueOutcome.Issued(voucher(null))
        val d = draft(VoucherType.FACTURA, "20123456789")
        coEvery { cashier.getBranchSeries() } returns Result.success(
            listOf(
                series(VoucherType.BOLETA, "B001", active = true),
                series(VoucherType.FACTURA, "F000", active = false),
                series(VoucherType.FACTURA, "F001", active = true),
                series(VoucherType.FACTURA, "F002", active = true)
            )
        )
        coEvery { cashier.issue(d, "F001") } returns Result.success(issued)

        assertSame(issued, IssueVoucherUseCase(cashier)(d).getOrThrow())
    }

    @Test
    fun `un comprobante con detalles pendientes pasa sin cambios`() = runTest {
        val pending = IssueOutcome.IssuedPendingDetails("v9")
        val d = draft(VoucherType.BOLETA, "45678912")
        coEvery { cashier.getBranchSeries() } returns Result.success(listOf(series(VoucherType.BOLETA, "B001", true)))
        coEvery { cashier.issue(d, "B001") } returns Result.success(pending)

        assertSame(pending, IssueVoucherUseCase(cashier)(d).getOrThrow())
    }

    @Test
    fun `preparar una orden que no esta completada falla con status`() = runTest {
        coEvery { workOrders.getDetail("wo1") } returns Result.success(detail(WorkOrderStatus.IN_PROGRESS, task(pen("100.00"))))

        val result = prepare()("wo1")

        assertEquals("status", field(result))
        coVerify(exactly = 0) { reception.getCustomer(any()) }
    }

    @Test
    fun `prepara el borrador con nombres del catalogo y del inventario`() = runTest {
        stubPrepare(
            detail(WorkOrderStatus.COMPLETED, task(pen("100.00"), listOf(product(pen("25.00"))))),
            Result.success(listOf(ServiceOption("s1", "Cambio de aceite", null, null)))
        )

        val draft = prepare()("wo1").getOrThrow()

        assertEquals(VoucherType.FACTURA, draft.type)
        assertEquals("20123456789", draft.customerInfo.taxId)
        assertEquals("PEN", draft.currency)
        assertEquals(listOf("Cambio de aceite", "Filtro de aceite"), draft.lines.map { it.description })
    }

    @Test
    fun `si el catalogo falla igual se arma el borrador con la descripcion`() = runTest {
        stubPrepare(detail(WorkOrderStatus.COMPLETED, task(pen("100.00"))), Result.failure(IOException("403")))

        val draft = prepare()("wo1").getOrThrow()

        assertEquals("Cambio de aceite", draft.lines.single().description)
    }

    @Test
    fun `monedas mezcladas no arman borrador`() = runTest {
        stubPrepare(
            detail(WorkOrderStatus.COMPLETED, task(pen("100.00"), listOf(product(Money(BigDecimal("25.00"), Money.USD))))),
            Result.success(emptyList())
        )

        assertEquals("currency", field(prepare()("wo1")))
    }

    @Test
    fun `pago que salda recarga la orden`() = runTest {
        val getDetail = GetWorkOrderDetailUseCase(workOrders)
        coEvery { cashier.registerPayment("v1", BigDecimal("118.00"), PaymentMethod.CASH, null) } returns
            Result.success(voucher(VoucherPaymentStatus.PAID))
        coEvery { workOrders.getDetail("wo1") } returns Result.failure(IOException("sin red"))

        val result = RegisterPaymentUseCase(cashier, getDetail)("v1", BigDecimal("118.00"), PaymentMethod.CASH)

        assertEquals(VoucherPaymentStatus.PAID, result.getOrThrow().paymentStatus)
        coVerify(exactly = 1) { workOrders.getDetail("wo1") }
    }

    @Test
    fun `pago parcial no recarga la orden`() = runTest {
        val getDetail = GetWorkOrderDetailUseCase(workOrders)
        coEvery { cashier.registerPayment("v1", BigDecimal("50.00"), PaymentMethod.CASH, null) } returns
            Result.success(voucher(VoucherPaymentStatus.PARTIAL))

        RegisterPaymentUseCase(cashier, getDetail)("v1", BigDecimal("50.00"), PaymentMethod.CASH).getOrThrow()

        coVerify(exactly = 0) { workOrders.getDetail(any()) }
    }
}
