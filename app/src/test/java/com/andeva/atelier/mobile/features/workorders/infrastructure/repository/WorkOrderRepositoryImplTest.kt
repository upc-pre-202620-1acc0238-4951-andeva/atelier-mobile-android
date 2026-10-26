package com.andeva.atelier.mobile.features.workorders.infrastructure.repository

import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.reception.infrastructure.ReceptionTestEnv
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.ProposalApproval
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkOrderRepositoryImplTest {

    private val env = ReceptionTestEnv()
    private lateinit var repository: WorkOrderRepositoryImpl

    @Before
    fun setUp() {
        env.start()
        repository = env.workOrderRepository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `la lista de la sede no guarda las ordenes cerradas`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders"] = env.json(
            listOf(env.workOrderSummary("wo1", "IN_PROGRESS"), env.workOrderSummary("wo2", "PAID"), env.workOrderSummary("wo3", "CANCELED"))
        )

        repository.refreshBranchWorkOrders().getOrThrow()

        assertEquals(listOf("wo1"), repository.observeBranchWorkOrders().first().map { it.id })
        assertEquals("b1", env.requested("GET", "/api/v1/work-orders").single().requestUrl!!.queryParameter("branchId"))
    }

    @Test
    fun `el detalle deja la cabecera con IGV y la lista no la borra`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task())))
        env.responses["GET /api/v1/work-orders"] = env.json(listOf(env.workOrderSummary(total = 118.0)))

        val detail = repository.getDetail("wo1").getOrThrow()
        repository.refreshBranchWorkOrders().getOrThrow()

        assertEquals(1, detail.tasks.size)
        assertEquals("Raspón", detail.intakeImages.single().description)
        val header = repository.observeWorkOrder("wo1").first()!!
        assertEquals(BigDecimal("18.0"), header.taxAmount!!.amount)
        assertEquals(BigDecimal("118.0"), header.totalAmount!!.amount)
        assertEquals("Ruido al frenar", header.diagnosticSummary)
        assertEquals(42000, header.mileageIn)
    }

    @Test
    fun `el supervisor ve las tareas con suspension pendiente`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(
            env.workOrderDetail(tasks = listOf(env.task("t1"), env.task("t2", pendingSuspensionRequestedAt = "2026-10-24T13:40:00Z")))
        )

        val detail = repository.getDetail("wo1").getOrThrow()

        assertEquals(listOf("t2"), detail.tasksAwaitingSuspensionReview.map { it.id.value })
    }

    @Test
    fun `abrir usa la sede de la sesion y un 409 trae el message`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders"] = env.json(env.workOrder(), 201)

        val created = repository.open(NewWorkOrder("v1", "c1", 42000, " Ruido al frenar ")).getOrThrow()

        val body = env.requested("POST", "/api/v1/work-orders").single().body.readUtf8()
        assertTrue(body.contains("\"branchId\":\"b1\""))
        assertTrue(body.contains("\"diagnosticSummary\":\"Ruido al frenar\""))
        assertEquals(WorkOrderStatus.DRAFT, created.status)
        assertEquals("wo1", repository.observeBranchWorkOrders().first().single().id)

        env.responses["POST /api/v1/work-orders"] = env.error(409, "CONFLICT", "El vehículo ya tiene una orden abierta")
        val error = repository.open(NewWorkOrder("v1", "c1", 42000)).exceptionOrNull() as ApiException
        assertEquals(409, error.httpStatus)
        assertEquals("El vehículo ya tiene una orden abierta", error.message)
    }

    @Test
    fun `agregar una tarea recarga el detalle con los totales nuevos`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders/wo1/tasks"] = env.json(env.task(), 201)
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task())))

        val detail = repository.addTask("wo1", NewWorkOrderTask("srv1", "Cambio de pastillas", mechanicId = "m9")).getOrThrow()

        assertEquals(1, detail.tasks.size)
        assertTrue(env.requested("POST", "/api/v1/work-orders/wo1/tasks").single().body.readUtf8().contains("\"mechanicId\":\"m9\""))
        assertEquals(BigDecimal("118.0"), repository.observeWorkOrder("wo1").first()!!.totalAmount!!.amount)
    }

    @Test
    fun `aprobar una propuesta crea la tarea y recarga la orden`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders/wo1/proposals/p1/approve"] = env.json(env.task("t9"))
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task("t9"))))

        val detail = repository.approveProposal("wo1", "p1", ProposalApproval("srv1", BigDecimal(80), BigDecimal("1.5"))).getOrThrow()

        assertEquals(listOf("t9"), detail.tasks.map { it.id.value })
    }

    @Test
    fun `rechazar la proforma cancela la orden y la saca de la lista`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders"] = env.json(env.workOrder(), 201)
        repository.open(NewWorkOrder("v1", "c1", 42000)).getOrThrow()
        env.responses["POST /api/v1/work-orders/wo1/quotation/reject"] = env.json(env.workOrder(status = "CANCELED"))

        val canceled = repository.rejectQuotation("wo1", "No acepta el precio").getOrThrow()

        assertEquals(WorkOrderStatus.CANCELED, canceled.status)
        assertNull(repository.observeWorkOrder("wo1").first())
        assertEquals("""{"reason":"No acepta el precio"}""", env.requested("POST", "/api/v1/work-orders/wo1/quotation/reject").single().body.readUtf8())
    }

    @Test
    fun `cancelar quita la orden de la cache de la sede`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders"] = env.json(env.workOrder(), 201)
        repository.open(NewWorkOrder("v1", "c1", 42000)).getOrThrow()
        env.responses["POST /api/v1/work-orders/wo1/cancel"] = env.json(env.workOrder(status = "CANCELED"))

        val canceled = repository.cancel("wo1", " Cliente desiste ").getOrThrow()

        assertEquals(WorkOrderStatus.CANCELED, canceled.status)
        assertNull(repository.observeWorkOrder("wo1").first())
        assertEquals("""{"reason":"Cliente desiste"}""", env.requested("POST", "/api/v1/work-orders/wo1/cancel").single().body.readUtf8())
    }

    @Test
    fun `iniciar y completar dejan la cabecera al dia`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders"] = env.json(env.workOrder(), 201)
        repository.open(NewWorkOrder("v1", "c1", 42000)).getOrThrow()
        env.responses["POST /api/v1/work-orders/wo1/start"] = env.json(env.workOrder(status = "IN_PROGRESS"))
        env.responses["POST /api/v1/work-orders/wo1/complete"] = env.json(env.workOrder(status = "COMPLETED"))

        assertEquals(WorkOrderStatus.IN_PROGRESS, repository.start("wo1").getOrThrow().status)
        assertEquals(WorkOrderStatus.IN_PROGRESS, repository.observeWorkOrder("wo1").first()!!.status)
        assertEquals(WorkOrderStatus.COMPLETED, repository.complete("wo1").getOrThrow().status)
        assertEquals(WorkOrderStatus.COMPLETED, repository.observeWorkOrder("wo1").first()!!.status)
    }

    @Test
    fun `transicion invalida devuelve el message del 422`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/work-orders/wo1/start"] = env.error(422, "UNPROCESSABLE_ENTITY", "La orden no puede iniciarse")
        env.responses["POST /api/v1/work-orders/wo1/complete"] = env.error(422, "UNPROCESSABLE_ENTITY", "Hay tareas sin terminar")
        env.responses["POST /api/v1/work-orders/wo1/cancel"] = env.error(422, "UNPROCESSABLE_ENTITY", "La orden ya esta pagada")

        assertEquals("La orden no puede iniciarse", repository.start("wo1").exceptionOrNull()!!.message)
        assertEquals("Hay tareas sin terminar", repository.complete("wo1").exceptionOrNull()!!.message)
        val canceled = repository.cancel("wo1", "Motivo").exceptionOrNull()!!
        assertEquals("La orden ya esta pagada", canceled.message)
        assertEquals(422, (canceled as ApiException).httpStatus)
    }

    @Test
    fun `la proforma se guarda en cacheDir pdfs y un 422 trae el message`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders/wo1/quotation/pdf"] = MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/pdf")
            .setHeader("Content-Disposition", "attachment; filename=\"proforma-15.pdf\"")
            .setBody("%PDF-1.4")

        val file = repository.downloadQuotationPdf("wo1").getOrThrow()

        assertEquals(File(env.context.cacheDir, "${PdfDownloader.DIRECTORY}/proforma-15.pdf"), file)
        assertEquals("%PDF-1.4", file.readText())

        env.responses["POST /api/v1/work-orders/wo1/quotation/send"] = env.error(422, "UNPROCESSABLE_ENTITY", "El cliente no tiene correo")
        assertEquals("El cliente no tiene correo", repository.sendQuotation("wo1").exceptionOrNull()!!.message)
    }

    @Test
    fun `catalogo de servicios se pide una vez y sin permiso no se pide`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/services"] = env.json(listOf(mapOf("id" to "srv1", "name" to "Frenos", "basePrice" to 100.0, "currency" to "PEN")))

        assertEquals("Frenos", repository.getServiceCatalog().getOrThrow().single().name)
        repository.getServiceCatalog().getOrThrow()
        assertEquals(1, env.requested("GET", "/api/v1/services").size)

        env.signIn(permissions = listOf(Permission.WORK_ORDERS_READ))
        assertTrue(env.workOrderRepository().getServiceCatalog().getOrThrow().isEmpty())
        assertTrue(repository.getBranchMechanics().getOrThrow().isEmpty())
        assertTrue(env.requested("GET", "/api/v1/hr/employees/branch/b1").isEmpty())
    }

    @Test
    fun `mecanicos de la sede con iam members read solo activos`() = runTest {
        env.signIn(permissions = listOf(Permission.MEMBERS_READ))
        env.responses["GET /api/v1/hr/employees/branch/b1"] = env.json(
            listOf(
                mapOf("id" to "e1", "branchId" to "b1", "membershipId" to "m9", "jobTitle" to "Mecánico", "employmentStatus" to "ACTIVE"),
                mapOf("id" to "e2", "branchId" to "b1", "membershipId" to "m8", "jobTitle" to "Mecánico", "employmentStatus" to "TERMINATED")
            )
        )

        assertEquals(listOf("m9"), repository.getBranchMechanics().getOrThrow().map { it.membershipId })
    }

    @Test
    fun `auditoria de reintegros y rentabilidad con ganancia negativa`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders/wo1/part-movements"] = env.json(
            listOf(mapOf("allocationId" to "al1", "productId" to "p1", "batchId" to "bt1", "chargedQuantity" to 3, "returnedQuantity" to 1,
                "unitCost" to 8.0, "currency" to "PEN", "lastReturnedBy" to "u2", "lastReturnReason" to "Sobró"))
        )
        env.responses["GET /api/v1/work-orders/wo1/profitability"] = env.json(
            mapOf("workOrderId" to "wo1", "partsRevenue" to 20.0, "partsCostOfSales" to 24.0, "partsGrossProfit" to -4.0,
                "currency" to "PEN", "lines" to listOf(mapOf("productId" to "p1", "quantity" to 2, "grossProfit" to -4.0)))
        )

        val movement = repository.getPartMovements("wo1").getOrThrow().single()
        val profitability = repository.getProfitability("wo1").getOrThrow()

        assertEquals(BigDecimal.ONE, movement.returnedQuantity.stripTrailingZeros())
        assertEquals("u2", movement.lastReturnedBy)
        assertEquals(BigDecimal("-4.0"), profitability.partsGrossProfit)
        assertEquals(BigDecimal("-4.0"), profitability.lines.single().grossProfit)
    }
}
