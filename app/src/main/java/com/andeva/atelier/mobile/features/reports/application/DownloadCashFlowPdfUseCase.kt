package com.andeva.atelier.mobile.features.reports.application

import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import com.andeva.atelier.mobile.features.reports.domain.ReportsRepository
import java.io.File
import javax.inject.Inject

/** PDF del flujo de caja de un periodo; la UI lo habilita con `invoicing:cashflow:export_pdf`. */
class DownloadCashFlowPdfUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    suspend operator fun invoke(period: ReportPeriod): Result<File> = reportsRepository.downloadCashFlowPdf(period)
}
