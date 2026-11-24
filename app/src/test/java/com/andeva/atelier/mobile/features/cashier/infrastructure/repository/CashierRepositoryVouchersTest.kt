package com.andeva.atelier.mobile.features.cashier.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.infrastructure.CashierTestEnv
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CashierRepositoryVouchersTest {

    private val env = CashierTestEnv()
    private lateinit var repository: CashierRepositoryImpl

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `getVouchers envia branchId de la sesion y filtros y cachea cabeceras`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/vouchers"] = env.json(
            listOf(env.voucher(id = "v1", type = "01"), env.voucher(id = "v2", type = "07"))
        )

        val result = repository.getVouchers(
            type = VoucherType.FACTURA,
            status = VoucherStatus.ACCEPTED_SUNAT,
            from = LocalDate.of(2026, 10, 1),
            to = LocalDate.of(2026, 10, 24)
        ).getOrThrow()

        assertEquals(listOf("v1", "v2"), result.map { it.id })
        val url = env.requested("GET", "/api/v1/invoicing/vouchers").single().requestUrl!!
        assertEquals("b1", url.queryParameter("branchId"))
        assertEquals("01", url.queryParameter("voucherType"))
        assertEquals("ACCEPTED_SUNAT", url.queryParameter("status"))
        assertEquals("2026-10-01", url.queryParameter("from"))
        assertEquals("2026-10-24", url.queryParameter("to"))
        assertEquals("v1", env.db.voucherDao().findById("v1")!!.id)
    }

    @Test
    fun `getVouchers sin sede falla con MissingBranchException`() = runTest {
        env.signIn(branchId = null)

        val error = repository.getVouchers().exceptionOrNull()

        assertTrue(error is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `voidVoucher envia motivo y actualiza la cache`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/invoicing/vouchers/v1/void"] = env.json(env.voucher(id = "v1", status = "VOIDED"))

        val voided = repository.voidVoucher("v1", "  Error en razón social  ").getOrThrow()

        assertEquals(VoucherStatus.VOIDED, voided.status)
        val body = JsonParser.parseString(
            env.requested("POST", "/api/v1/invoicing/vouchers/v1/void").single().body.readUtf8()
        ).asJsonObject
        assertEquals("Error en razón social", body["reason"].asString)
        assertEquals("VOIDED", env.db.voucherDao().findById("v1")!!.status)
    }

    @Test
    fun `voidVoucher devuelve el message del backend si vencio la ventana de 7 dias`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/invoicing/vouchers/v1/void"] =
            env.error(422, "ERR_VOID_WINDOW_EXPIRED", "El plazo de 7 días calendario ha vencido")

        val error = repository.voidVoucher("v1", "Error de emisión").exceptionOrNull()

        assertEquals(422, (error as ApiException).httpStatus)
        assertEquals("El plazo de 7 días calendario ha vencido", error.message)
    }

    @Test
    fun `voidVoucher con motivo en blanco falla sin llamar al backend`() = runTest {
        env.signIn()

        val error = repository.voidVoucher("v1", "   ").exceptionOrNull()

        assertEquals("reason", (error as InvalidCashierInputException).field)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `issueCreditNote envia IssueCreditNoteRequest exacto para nota parcial y total`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/invoicing/vouchers/v1"] = env.json(env.voucher(id = "v1", type = "01", total = 118.0))
        val reference = repository.getVoucher("v1").getOrThrow()
        env.responses["POST /api/v1/invoicing/vouchers/credit-notes"] =
            env.json(env.voucher(id = "nc1", type = "07", total = 50.0), code = 201)

        val partialNc = repository.issueCreditNote(
            referenceVoucher = reference,
            serie = "FC01",
            reason = "Descuento por demora",
            amount = BigDecimal("50.00")
        ).getOrThrow()

        assertEquals(VoucherType.NOTA_CREDITO, partialNc.type)
        val body = JsonParser.parseString(
            env.requested("POST", "/api/v1/invoicing/vouchers/credit-notes").single().body.readUtf8()
        ).asJsonObject
        assertEquals("b1", body["branchId"].asString)
        assertEquals("c1", body["customerId"].asString)
        assertEquals("v1", body["referenceVoucherId"].asString)
        assertEquals("FC01", body["serie"].asString)
        assertEquals("Descuento por demora", body["reason"].asString)
        assertEquals("Descuento por demora", body["reasonDescription"].asString)
        val line = body["lines"].asJsonArray.single().asJsonObject
        assertEquals("50.00", line["unitPriceWithIgv"].asBigDecimal.toPlainString())
        assertEquals("nc1", env.db.voucherDao().findById("nc1")!!.id)
    }
}
