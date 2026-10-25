package com.andeva.atelier.mobile.features.cashier.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.infrastructure.CashierTestEnv
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
import java.io.IOException
import java.math.BigDecimal

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
        assertEquals("v1", outcome.voucher.id)
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
        assertEquals("v9", outcome.voucher.id)
        assertTrue(env.requested("POST", issuePath).isEmpty())
    }

    @Test
    fun `una nota de credito no bloquea una nueva emision`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "nc", type = "07")))
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new"), code = 201)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertEquals("new", outcome.voucher.id)
        assertEquals(1, env.requested("POST", issuePath).size)
    }

    @Test
    fun `un comprobante anulado no bloquea una nueva emision`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "old", status = "VOIDED")))
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new"), code = 201)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("new", outcome.voucher.id)
        assertEquals(1, env.requested("POST", issuePath).size)
    }


    @Test
    fun `un comprobante rechazado por SUNAT no bloquea una nueva emision`() = runTest {
        env.signIn()
        env.responses["GET $listPath"] = env.json(listOf(env.voucher(id = "rej", status = "REJECTED_SUNAT")))
        env.responses["POST $issuePath"] = env.json(env.voucher(id = "new"), code = 201)

        val outcome = repository.issue(env.draft(), "B001").getOrThrow()

        assertTrue(outcome is IssueOutcome.Issued)
        assertEquals("new", outcome.voucher.id)
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
        assertEquals("created", outcome.voucher.id)
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
}
