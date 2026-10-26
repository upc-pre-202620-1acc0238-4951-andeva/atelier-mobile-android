package com.andeva.atelier.mobile.features.reports.application

import com.andeva.atelier.mobile.features.reports.domain.InvalidReportInputException
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import com.andeva.atelier.mobile.features.reports.domain.ReportsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportUseCasesTest {
    private val repository = mockk<ReportsRepository>()
    private val period = ReportPeriod.month(YearMonth.of(2026, 3))

    @Test
    fun `sin red el pdf de flujo de caja falla con IOException`() = runTest {
        val error = IOException("sin red")
        coEvery { repository.downloadCashFlowPdf(period) } returns Result.failure(error)

        val result = DownloadCashFlowPdfUseCase(repository)(period)

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `sin red el pdf de flota falla con IOException`() = runTest {
        val error = IOException("sin red")
        coEvery { repository.downloadFleetHealthPdf("c-1") } returns Result.failure(error)

        val result = DownloadFleetHealthPdfUseCase(repository)("c-1")

        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `el pdf de flota entrega el archivo del repositorio`() = runTest {
        val file = File("fleet.pdf")
        coEvery { repository.downloadFleetHealthPdf("c-1") } returns Result.success(file)

        assertEquals(file, DownloadFleetHealthPdfUseCase(repository)("c-1").getOrThrow())
        coVerify(exactly = 1) { repository.downloadFleetHealthPdf("c-1") }
    }

    @Test
    fun `un cliente de flota en blanco falla sin llamar al repositorio`() = runTest {
        val result = DownloadFleetHealthPdfUseCase(repository)("  ")

        assertEquals("customerId", (result.exceptionOrNull() as InvalidReportInputException).field)
        coVerify(exactly = 0) { repository.downloadFleetHealthPdf(any()) }
    }

    @Test
    fun `el resumen de flujo de caja propaga el fallo sin cambios`() = runTest {
        val error = IOException("sin red")
        coEvery { repository.getCashFlow(period) } returns Result.failure(error)

        assertSame(error, GetCashFlowSummaryUseCase(repository)(period).exceptionOrNull())
    }
}
