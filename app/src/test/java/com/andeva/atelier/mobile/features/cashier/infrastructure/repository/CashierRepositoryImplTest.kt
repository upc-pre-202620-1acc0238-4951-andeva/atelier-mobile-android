package com.andeva.atelier.mobile.features.cashier.infrastructure.repository

import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.PaymentOutcomeUnknownException
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherEntity
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.infrastructure.CashierTestEnv
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
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
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CashierRepositoryImplTest {

    private val env = CashierTestEnv()
    private lateinit var repository: CashierRepositoryImpl

    private val listPath = "/api/v1/invoicing/vouchers/work-order/wo1"
    private val issuePath = "/api/v1/invoicing/vouchers"

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `emitir envia voucherType como codigo SUNAT y la sede de la sesion`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(emptyList<Any>())
        env.responses["POST $issuePath"] = env.json(env.voucher(type = "01"), code = 201)

        val outcome = repository.issue(env.draft(VoucherType.FACTURA), "F001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("v1", outcome.voucherId)
        val body = JsonParser.parseString(env.requested("POST", issuePath).single().body.readUtf8()).asJsonObject
        assertEquals("01", body["voucherType"].asString)
        assertEquals("b1", body["branchId"].asString)
        assertEquals("F001", body["serie"].asString)
        assertEquals("wo1", body["workOrderId"].asString)
        assertEquals("PEN", body["currency"].asString)
        assertEquals("DNI", body["customerInfo"].asJsonObject["documentType"].asString)
        val line = body["lines"].asJsonArray.single().asJsonObject
        assertEquals("SERVICE", line["itemType"].asString)
        assertEquals("118.00", line["unitPriceWithIgv"].asBigDecimal.toPlainString())
        assertEquals("v1", env.db.voucherDao().findById("v1")!!.id)
    }

    @Test
    fun `si la orden ya tiene un comprobante activo no emite otro`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "v9", status = "ISSUED")))

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.AlreadyIssued)
        assertEquals("v9", outcome.voucherId)
        assertTrue(env.requested("POST", issuePath).isEmpty())
    }

    @Test
    fun `una nota de credito no bloquea una nueva emision`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "nc", type = "07")))
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new"), code = 201)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertEquals("new", outcome.voucherId)
        assertEquals(1, env.requested("POST", issuePath).size)
    }

    @Test
    fun `un comprobante anulado no bloquea una nueva emision`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "old", status = "VOIDED")))
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new"), code = 201)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("new", outcome.voucherId)
        assertEquals(1, env.requested("POST", issuePath).size)
    }


    @Test
    fun `un comprobante rechazado por SUNAT no bloquea una nueva emision`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "rej", status = "REJECTED_SUNAT")))
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new"), code = 201)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("new", outcome.voucherId)
        assertEquals(1, env.requested("POST", issuePath).size)
    }

    @Test
    fun `una moneda desconocida conserva el saldo pendiente`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/vouchers/v1"] = env.json(env.voucher(paid = 18.0) + ("currency" to "EUR"))

        val voucher = repository.getVoucher("v1").getOrThrow()

        assertEquals(0, BigDecimal("100.0").compareTo(voucher.pendingBalance.amount))
        assertEquals("PEN", voucher.pendingBalance.currency)
    }

    @Test
    fun `un monto invalido hace fallar el mapeo en vez de inventar un saldo en cero`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/vouchers/v1"] = env.json(env.voucher() + ("pendingBalance" to -5.0))

        assertTrue(repository.getVoucher("v1").isFailure)
    }
    @Test
    fun `tras un timeout al emitir relee y devuelve el comprobante creado`() = runTest {
        env.signIn()
        env.enqueue("GET $listPath", env.json(emptyList<Any>()), env.json(listOf(env.voucher(id = "created"))))
        env.responses["POST $issuePath"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("created", outcome.voucherId)
        assertEquals(1, env.requested("POST", issuePath).size)
        assertEquals(2, env.requested("GET", listPath).size)
    }

    @Test
    fun `tras un timeout sin comprobante devuelve la IOException original`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(emptyList<Any>())
        env.responses["POST $issuePath"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

        val error = repository.issue(env.draft(), "B001").exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(1, env.requested("POST", issuePath).size)
        assertNull(env.db.voucherDao().findById("v1"))
    }

    @Test
    fun `502 de SUNAT devuelve el message y no guarda nada`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(emptyList<Any>())
        env.responses["POST $issuePath"] = env.error(502, "SUNAT_ERROR", "SUNAT no responde")

        val error = repository.issue(env.draft(), "B001").exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals(502, (error as ApiException).httpStatus)
        assertEquals("SUNAT no responde", error.message)
        assertEquals(2, env.requests.size)
        assertTrue(env.db.voucherDao().observeByWorkOrder("wo1").first().isEmpty())
    }

    @Test
    fun `si no se puede verificar la orden no emite`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.error(403, "FORBIDDEN", "Sin permiso")

        val error = repository.issue(env.draft(), "B001").exceptionOrNull()

        assertEquals("Sin permiso", error?.message)
        assertTrue(env.requested("POST", issuePath).isEmpty())
    }

    @Test
    fun `sin sede emitir falla con MissingBranchException sin llamar al backend`() = runTest {
        env.signIn(branchId = null)

        val error = repository.issue(env.draft(), "B001").exceptionOrNull()

        assertTrue(error is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `refrescar reemplaza las cabeceras de la orden y devuelve los comprobantes completos`() = runTest {
        env.signIn()
        env.db.voucherDao().upsert(com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherEntity(
            "stale", "wo1", "03", "B001", 1, "10", "0", "10", "PENDING", "ISSUED", null, 0
        ))
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "v1", paid = 18.0), env.voucher(id = "v2", status = "VOIDED")))

        val vouchers = repository.refreshWorkOrderVouchers("wo1").getOrThrow()

        assertEquals(listOf("v1", "v2"), vouchers.map { it.id })
        assertEquals("100.00".toBigDecimal().compareTo(vouchers[0].pendingBalance.amount), 0)
        val cached = repository.observeWorkOrderVouchers("wo1").first()
        assertEquals(listOf("v1", "v2"), cached.map { it.id })
        assertTrue(cached[0].isActiveSale)
        assertTrue(!cached[1].isActiveSale)
        assertEquals("https://cdn/b001-7.pdf", cached[0].pdfUrl)
    }

    @Test
    fun `getVoucher lee de la red y deja la cabecera en cache`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/vouchers/v1"] = env.json(env.voucher())

        val voucher = repository.getVoucher("v1").getOrThrow()

        assertEquals(VoucherType.BOLETA, voucher.type)
        assertEquals("wo1", env.db.voucherDao().findById("v1")!!.workOrderId)
    }

    @Test
    fun `las series salen de la sede de la sesion`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/series-configurations/branch/b1"] = env.json(
            listOf(mapOf("id" to "s1", "branchId" to "b1", "voucherType" to "01", "serie" to "F001", "currentCorrelative" to 4, "active" to true))
        )

        val series = repository.getBranchSeries().getOrThrow()

        assertEquals(VoucherType.FACTURA, series.single().type)
        assertEquals("F001", series.single().serie)
        assertTrue(series.single().active)
    }

    @Test
    fun `sin sede las series fallan con MissingBranchException`() = runTest {
        env.signIn(branchId = null)

        assertTrue(repository.getBranchSeries().exceptionOrNull() is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    // --- Series SUNAT (B5) ---

    private val seriesPath = "/api/v1/invoicing/series-configurations"

    private fun seriesJson(id: String = "s1", type: String = "03", serie: String = "B001", active: Boolean = true) =
        mapOf("id" to id, "branchId" to "b1", "voucherType" to type, "serie" to serie, "currentCorrelative" to 0, "active" to active)

    @Test
    fun `configurar envia el codigo SUNAT`() = runTest {
        env.signIn()
        env.responses["POST $seriesPath"] = env.json(seriesJson(), code = 201)

        val series = repository.configureSeries(VoucherType.BOLETA, "B001", 5).getOrThrow()

        assertEquals("s1", series.id)
        assertEquals(VoucherType.BOLETA, series.type)
        val body = JsonParser.parseString(env.requested("POST", seriesPath).single().body.readUtf8()).asJsonObject
        assertEquals("03", body["voucherType"].asString)
        assertEquals("B001", body["serie"].asString)
        assertEquals("b1", body["branchId"].asString)
        assertEquals(5, body["initialCorrelative"].asInt)
    }

    @Test
    fun `serie con prefijo invalido no llega al backend`() = runTest {
        env.signIn()

        val error = repository.configureSeries(VoucherType.BOLETA, "F001", null).exceptionOrNull()

        assertTrue(error is InvalidCashierInputException)
        assertEquals("serie", (error as InvalidCashierInputException).field)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `un correlativo inicial negativo no llega al backend`() = runTest {
        env.signIn()

        val error = repository.configureSeries(VoucherType.FACTURA, "F001", -1).exceptionOrNull()

        assertEquals("initialCorrelative", (error as InvalidCashierInputException).field)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `sin sede configurar falla con MissingBranchException sin llamar al backend`() = runTest {
        env.signIn(branchId = null)

        assertTrue(repository.configureSeries(VoucherType.BOLETA, "B001", null).exceptionOrNull() is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `serie duplicada devuelve el message del 409`() = runTest {
        env.signIn()
        env.responses["POST $seriesPath"] = env.error(409, "SERIES_EXISTS", "La serie ya está registrada")

        val error = repository.configureSeries(VoucherType.BOLETA, "B001", null).exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals(409, (error as ApiException).httpStatus)
        assertEquals("La serie ya está registrada", error.message)
    }

    @Test
    fun `activar o desactivar una serie envia active`() = runTest {
        env.signIn()
        env.responses["PUT $seriesPath/s1/status"] = MockResponse().setResponseCode(200).setBody("{}")

        repository.setSeriesActive("s1", false).getOrThrow()

        val body = JsonParser.parseString(env.requested("PUT", "$seriesPath/s1/status").single().body.readUtf8()).asJsonObject
        assertEquals(false, body["active"].asBoolean)
    }

    // --- Cobros (B4) ---

    private val voucherPath = "/api/v1/invoicing/vouchers/v1"
    private val paymentsPath = "/api/v1/invoicing/payments"

    private fun paymentBodies() =
        env.requested("POST", paymentsPath).map { JsonParser.parseString(it.body.readUtf8()).asJsonObject }

    @Test
    fun `un monto mayor al saldo no llega al backend`() = runTest {
        env.signIn()
        env.responses["GET $voucherPath"] = env.json(env.voucher(paid = 50.0))

        val error = repository.registerPayment("v1", BigDecimal("68.01"), PaymentMethod.CASH, null).exceptionOrNull()

        assertEquals("amount", (error as InvalidCashierInputException).field)
        assertTrue(env.requested("POST", paymentsPath).isEmpty())
    }

    @Test
    fun `monto menor a un centimo o referencia de mas de 100 no llegan al backend`() = runTest {
        env.signIn()
        env.responses["GET $voucherPath"] = env.json(env.voucher())

        val zero = repository.registerPayment("v1", BigDecimal("0.00"), PaymentMethod.CASH, null).exceptionOrNull()
        val tiny = repository.registerPayment("v1", BigDecimal("0.009"), PaymentMethod.CASH, null).exceptionOrNull()
        val thirdDecimal = repository.registerPayment("v1", BigDecimal("10.005"), PaymentMethod.CASH, null).exceptionOrNull()
        val longReference =
            repository.registerPayment("v1", BigDecimal("10"), PaymentMethod.BANK_TRANSFER, "x".repeat(101)).exceptionOrNull()

        assertEquals("amount", (zero as InvalidCashierInputException).field)
        assertEquals("amount", (tiny as InvalidCashierInputException).field)
        assertEquals("amount", (thirdDecimal as InvalidCashierInputException).field)
        assertEquals("transactionReference", (longReference as InvalidCashierInputException).field)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `sin sede cobrar falla con MissingBranchException sin llamar al backend`() = runTest {
        env.signIn(branchId = null)

        val error = repository.registerPayment("v1", BigDecimal("10"), PaymentMethod.CASH, null).exceptionOrNull()

        assertTrue(error is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `pago parcial deja PARTIAL y pago final deja PAID con el saldo del comprobante`() = runTest {
        env.signIn()
        env.enqueue(
            "GET $voucherPath",
            env.json(env.voucher()),
            env.json(env.voucher(paid = 50.0)),
            env.json(env.voucher(paid = 50.0)),
            env.json(env.voucher(paid = 118.0) + ("paymentStatus" to "PAID"))
        )
        env.enqueue(
            "POST $paymentsPath",
            env.json(env.payment(amount = 50.0), code = 201),
            env.json(env.payment("p2", 68.0), code = 201)
        )

        val partial = repository.registerPayment("v1", BigDecimal("50.00"), PaymentMethod.CASH, null).getOrThrow()
        val final = repository.registerPayment("v1", BigDecimal("68.00"), PaymentMethod.DIGITAL_WALLET_YAPE, " OP-123 ").getOrThrow()

        assertEquals(VoucherPaymentStatus.PARTIAL, partial.paymentStatus)
        assertEquals(0, BigDecimal("68").compareTo(partial.pendingBalance.amount))
        assertEquals(VoucherPaymentStatus.PAID, final.paymentStatus)
        assertEquals(0, BigDecimal.ZERO.compareTo(final.pendingBalance.amount))
        val (first, second) = paymentBodies()
        assertEquals("b1", first["branchId"].asString)
        assertEquals("PEN", first["currency"].asString)
        assertEquals("v1", first["voucherId"].asString)
        assertEquals("CASH", first["paymentMethod"].asString)
        assertEquals("50.00", first["amount"].asBigDecimal.toPlainString())
        assertTrue(first["transactionReference"] == null || first["transactionReference"].isJsonNull)
        assertEquals("OP-123", second["transactionReference"].asString)
        assertEquals(0, BigDecimal("118").compareTo(env.db.voucherDao().findById("v1")!!.paidAmount.toBigDecimal()))
    }

    @Test
    fun `timeout al cobrar no reintenta y falla con PaymentOutcomeUnknownException tras releer`() = runTest {
        env.signIn()
        env.enqueue("GET $voucherPath", env.json(env.voucher()), env.json(env.voucher(paid = 118.0)))
        env.responses["POST $paymentsPath"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

        val error = repository.registerPayment("v1", BigDecimal("118"), PaymentMethod.CASH, null).exceptionOrNull()

        assertTrue(error is PaymentOutcomeUnknownException)
        assertTrue(error?.cause is IOException)
        assertEquals(1, env.requested("POST", paymentsPath).size)
        assertEquals(2, env.requested("GET", voucherPath).size)
        assertEquals(0, BigDecimal("118").compareTo(env.db.voucherDao().findById("v1")!!.paidAmount.toBigDecimal()))
    }

    @Test
    fun `un error HTTP al cobrar devuelve el message y no relee`() = runTest {
        env.signIn()
        env.responses["GET $voucherPath"] = env.json(env.voucher())
        env.responses["POST $paymentsPath"] = env.error(409, "CONFLICT", "El comprobante ya está pagado")

        val error = repository.registerPayment("v1", BigDecimal("10"), PaymentMethod.CASH, null).exceptionOrNull()

        assertEquals("El comprobante ya está pagado", (error as ApiException).message)
        assertEquals(1, env.requested("GET", voucherPath).size)
    }

    @Test
    fun `el historial de cobros sale de la red`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/payments/voucher/v1"] =
            env.json(listOf(env.payment(), env.payment("p2", 68.0, "DIGITAL_WALLET_PLIN")))

        val payments = repository.getPayments("v1").getOrThrow()

        assertEquals(listOf(PaymentMethod.CASH, PaymentMethod.DIGITAL_WALLET_PLIN), payments.map { it.method })
        assertEquals(0, BigDecimal("68").compareTo(payments[1].amount.amount))
    }

    // --- Caja del día ---

    @Test
    fun `la caja diaria usa la sede de la sesion y la fecha pedida`() = runTest {
        env.signIn()
        val dailyPath = "/api/v1/invoicing/payments/branch/b1/daily"
        env.responses["GET $dailyPath"] = env.json(
            mapOf(
                "branchId" to "b1", "date" to "2026-10-24", "totalCollected" to 118.0, "currency" to "PEN", "paymentCount" to 2,
                "breakdownByMethod" to mapOf("CASH" to 50.0, "DIGITAL_WALLET_YAPE" to 68.0, "OTRO" to 1.0),
                "payments" to listOf(env.payment(), env.payment("p2", 68.0, "DIGITAL_WALLET_YAPE"))
            )
        )

        val summary = repository.getDailyCashSummary(LocalDate.of(2026, 10, 24)).getOrThrow()

        assertEquals("date=2026-10-24", env.requested("GET", dailyPath).single().requestUrl!!.query)
        assertEquals(LocalDate.of(2026, 10, 24), summary.date)
        assertEquals(0, BigDecimal("118").compareTo(summary.totalCollected.amount))
        assertEquals(2, summary.paymentCount)
        assertEquals(setOf(PaymentMethod.CASH, PaymentMethod.DIGITAL_WALLET_YAPE), summary.byMethod.keys)
        assertEquals(2, summary.payments.size)
    }

    @Test
    fun `caja diaria sin sede falla con MissingBranchException`() = runTest {
        env.signIn(branchId = null)

        assertTrue(repository.getDailyCashSummary(LocalDate.of(2026, 10, 24)).exceptionOrNull() is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    // --- Pase de salida y entrega ---

    private suspend fun seedWorkOrder(status: String = "COMPLETED") {
        val dto = env.gson.fromJson(env.gson.toJson(env.workOrder(status = status)), WorkOrderDto::class.java)
        env.db.workOrderDao().saveHeader(dto.toEntity(0))
    }

    @Test
    fun `pase de salida de una orden sin pagar devuelve el message del 422`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders/wo1/exit-pass/pdf"] = env.error(422, "UNPROCESSABLE", "La orden aún no está pagada")

        val error = repository.downloadExitPass("wo1").exceptionOrNull()

        assertEquals(422, (error as ApiException).httpStatus)
        assertEquals("La orden aún no está pagada", error.message)
    }

    @Test
    fun `el pase de salida se guarda en cacheDir pdfs`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-orders/wo1/exit-pass/pdf"] = MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/pdf")
            .setHeader("Content-Disposition", "attachment; filename=\"pase-salida-15.pdf\"")
            .setBody("%PDF-1.4")

        val file = repository.downloadExitPass("wo1").getOrThrow()

        assertEquals(File(env.context.cacheDir, "${PdfDownloader.DIRECTORY}/pase-salida-15.pdf"), file)
        assertEquals("%PDF-1.4", file.readText())
    }

    @Test
    fun `entregar guarda la cabecera y la orden sale de la cache de la sede`() = runTest {
        env.signIn()
        seedWorkOrder()
        env.responses["POST /api/v1/work-orders/wo1/deliver"] = env.json(env.workOrder(status = "PAID"))

        val order = repository.deliver("wo1").getOrThrow()

        assertEquals(WorkOrderStatus.PAID, order.status)
        assertNull(env.db.workOrderDao().findById("wo1"))
    }

    @Test
    fun `marcar como pagada guarda la orden abierta y saca la cerrada de la cache`() = runTest {
        env.signIn()
        seedWorkOrder("IN_PROGRESS")
        env.responses["POST /api/v1/work-orders/wo1/mark-as-paid"] = env.json(env.workOrder(status = "COMPLETED"))

        repository.markWorkOrderPaid("wo1").getOrThrow()
        assertEquals("COMPLETED", env.db.workOrderDao().findById("wo1")!!.status)

        env.responses["POST /api/v1/work-orders/wo1/mark-as-paid"] = env.json(env.workOrder(status = "PAID"))
        repository.markWorkOrderPaid("wo1").getOrThrow()
        assertNull(env.db.workOrderDao().findById("wo1"))
    }

    @Test
    fun `un error al entregar devuelve el message y no toca la cache`() = runTest {
        env.signIn()
        seedWorkOrder()
        env.responses["POST /api/v1/work-orders/wo1/deliver"] = env.error(422, "UNPROCESSABLE", "Falta el pago")

        val error = repository.deliver("wo1").exceptionOrNull()

        assertEquals("Falta el pago", error?.message)
        assertEquals("COMPLETED", env.db.workOrderDao().findById("wo1")!!.status)
    }

    // --- Montos y moneda nunca inventados ---

    @Test
    fun `un saldo pendiente ausente hace fallar la lectura en vez de verse pagado`() = runTest {
        env.signIn()
        env.responses["GET $voucherPath"] = env.json(env.voucher() - "pendingBalance")

        assertTrue(repository.getVoucher("v1").isFailure)
        assertTrue(repository.registerPayment("v1", BigDecimal("10"), PaymentMethod.CASH, null).isFailure)
        assertTrue(env.requested("POST", paymentsPath).isEmpty())
    }

    @Test
    fun `una moneda desconocida en el comprobante no envia un cobro con PEN inventado`() = runTest {
        env.signIn()
        env.responses["GET $voucherPath"] = env.json(env.voucher() + ("currency" to "EUR"))

        val error = repository.registerPayment("v1", BigDecimal("10"), PaymentMethod.CASH, null).exceptionOrNull()

        assertEquals("currency", (error as InvalidCashierInputException).field)
        assertTrue(env.requested("POST", paymentsPath).isEmpty())
    }

    @Test
    fun `cobro registrado pero relectura fallida falla con PaymentOutcomeUnknownException`() = runTest {
        env.signIn()
        env.enqueue("GET $voucherPath", env.json(env.voucher()), env.error(500, "ERROR", "Falla"))
        env.responses["POST $paymentsPath"] = env.json(env.payment(amount = 10.0), code = 201)

        val error = repository.registerPayment("v1", BigDecimal("10"), PaymentMethod.CASH, null).exceptionOrNull()

        assertTrue(error is PaymentOutcomeUnknownException)
        assertEquals(1, env.requested("POST", paymentsPath).size)
    }

    // --- Emision con respuesta incompleta y cache sin montos inventados ---

    @Test
    fun `una respuesta de emision sin saldo se relee por id y sigue siendo Issued`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(emptyList<Any>())
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new") - "pendingBalance", code = 201)
        env.responses["GET /api/v1/invoicing/vouchers/new"] = env.json(env.voucher(id = "new"))

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("new", outcome.voucherId)
        assertEquals(1, env.requested("POST", issuePath).size)
    }

    @Test
    fun `si ni la respuesta ni la relectura se pueden mapear devuelve IssuedPendingDetails`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(emptyList<Any>())
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new") - "pendingBalance", code = 201)
        env.responses["GET /api/v1/invoicing/vouchers/new"] = env.error(500, "ERROR", "Falla")

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertEquals(IssueOutcome.IssuedPendingDetails("new"), outcome)
        assertEquals(1, env.requested("POST", issuePath).size)
        assertNull(env.db.voucherDao().findById("new"))
    }

    @Test
    fun `refrescar no guarda comprobantes sin montos`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "ok"), env.voucher(id = "bad") - "totalAmount"))

        repository.refreshWorkOrderVouchers("wo1")

        assertEquals(listOf("ok"), repository.observeWorkOrderVouchers("wo1").first().map { it.id })
    }

    @Test
    fun `una fila de cache con monto ilegible no se muestra como pagada`() = runTest {
        val dao = env.db.voucherDao()
        dao.upsert(VoucherEntity("good", "wo1", "03", "B001", 1, "118", "0", "118", "PENDING", "ISSUED", null, 0))
        dao.upsert(VoucherEntity("bad", "wo1", "03", "B001", 2, "118", "0", "", "PENDING", "ISSUED", null, 0))

        assertEquals(listOf("good"), repository.observeWorkOrderVouchers("wo1").first().map { it.id })
    }
}
