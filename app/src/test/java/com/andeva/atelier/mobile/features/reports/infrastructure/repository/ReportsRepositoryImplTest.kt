package com.andeva.atelier.mobile.features.reports.infrastructure.repository

import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.cashier.infrastructure.CashierTestEnv
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import com.andeva.atelier.mobile.features.reports.infrastructure.remote.ReportService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
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
class ReportsRepositoryImplTest {

    private val env = CashierTestEnv()
    private lateinit var repository: ReportsRepositoryImpl
    private val period = ReportPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))

    @Before
    fun setUp() {
        env.start()
        repository = ReportsRepositoryImpl(
            reportService = env.api(ReportService::class.java),
            pdfDownloader = PdfDownloader(env.context, env.gson),
            gson = env.gson
        )
    }

    @After
    fun tearDown() = env.stop()

    private fun summary(totalIncome: Double? = 1180.5, currency: String? = "PEN") = mapOf(
        "totalIncome" to totalIncome, "totalExpenses" to 200.0, "accountsReceivable" to 50.0, "netCashFlow" to -20.25,
        "currency" to currency,
        "movements" to listOf(
            mapOf(
                "transactionId" to "tx1", "movementDate" to "2026-10-05T15:00:00Z", "type" to "INCOME", "category" to "SALES",
                "concept" to "Boleta B001-7", "referenceNumber" to "B001-7", "amount" to 118.0, "runningBalance" to 118.0
            )
        )
    )

    @Test
    fun `el flujo de caja envia from y to y mapea el resumen`() = runTest {
        env.responses["GET /api/v1/invoicing/financial-reports/cash-flow"] = env.json(summary())

        val result = repository.getCashFlow(period).getOrThrow()

        val url = env.requests.single().requestUrl!!
        assertEquals("2026-10-01", url.queryParameter("from"))
        assertEquals("2026-10-31", url.queryParameter("to"))
        assertEquals(0, BigDecimal("1180.5").compareTo(result.totalIncome.amount))
        assertEquals("PEN", result.totalIncome.currency)
        assertEquals(0, BigDecimal("-20.25").compareTo(result.netCashFlow))
        assertEquals("tx1", result.movements.single().transactionId)
    }

    @Test
    fun `un importe que falta es un fallo y no un cero`() = runTest {
        env.responses["GET /api/v1/invoicing/financial-reports/cash-flow"] = env.json(summary(totalIncome = null))

        assertTrue(repository.getCashFlow(period).isFailure)
    }

    @Test
    fun `una moneda que falta es un fallo y no PEN`() = runTest {
        env.responses["GET /api/v1/invoicing/financial-reports/cash-flow"] = env.json(summary(currency = null))

        assertTrue(repository.getCashFlow(period).isFailure)
    }

    @Test
    fun `una moneda desconocida es un fallo y no PEN`() = runTest {
        env.responses["GET /api/v1/invoicing/financial-reports/cash-flow"] = env.json(summary(currency = "EUR"))

        assertTrue(repository.getCashFlow(period).isFailure)
    }

    @Test
    fun `el pdf de flujo de caja envia from y to en yyyy-MM-dd`() = runTest {
        env.responses["GET /api/v1/invoicing/financial-reports/cash-flow/pdf"] = MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/pdf")
            .setBody("%PDF-1.4")

        val file = repository.downloadCashFlowPdf(period).getOrThrow()

        val url = env.requests.single().requestUrl!!
        assertEquals("2026-10-01", url.queryParameter("from"))
        assertEquals("2026-10-31", url.queryParameter("to"))
        assertEquals("%PDF-1.4", file.readText())
    }

    @Test
    fun `flota de un cliente de otro taller devuelve el message del 404`() = runTest {
        env.responses["GET /api/v1/iot/health-reports/fleet/pdf"] = env.error(404, "NOT_FOUND", "Cliente no encontrado en el taller")

        val error = repository.downloadFleetHealthPdf("c9").exceptionOrNull()

        assertEquals(404, (error as ApiException).httpStatus)
        assertEquals("Cliente no encontrado en el taller", error.message)
        assertEquals("c9", env.requests.single().requestUrl!!.queryParameter("customerId"))
    }
}
