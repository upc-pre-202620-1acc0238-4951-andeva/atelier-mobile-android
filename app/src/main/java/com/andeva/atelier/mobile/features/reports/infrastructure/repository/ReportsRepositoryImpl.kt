package com.andeva.atelier.mobile.features.reports.infrastructure.repository

import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.features.reports.domain.CashFlowSummary
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import com.andeva.atelier.mobile.features.reports.domain.ReportsRepository
import com.andeva.atelier.mobile.features.reports.infrastructure.remote.ReportService
import com.andeva.atelier.mobile.features.reports.infrastructure.remote.toDomain
import com.google.gson.Gson
import java.io.File
import javax.inject.Inject

class ReportsRepositoryImpl @Inject constructor(
    private val reportService: ReportService,
    private val pdfDownloader: PdfDownloader,
    private val gson: Gson
) : ReportsRepository {

    override suspend fun getCashFlow(period: ReportPeriod): Result<CashFlowSummary> =
        apiCall(gson, { reportService.getCashFlow(period.from.toString(), period.to.toString()) }) { it }
            // Un importe, fecha o moneda que faltan o no valen llegan como fallo, no como excepción.
            .mapCatching { it.toDomain() }

    override suspend fun downloadCashFlowPdf(period: ReportPeriod): Result<File> =
        pdfDownloader.download("flujo-de-caja-${period.from}-${period.to}") {
            reportService.getCashFlowPdf(period.from.toString(), period.to.toString())
        }

    override suspend fun downloadFleetHealthPdf(customerId: String): Result<File> =
        pdfDownloader.download("salud-flota-$customerId") { reportService.getFleetHealthPdf(customerId) }
}
