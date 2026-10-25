package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import java.io.File
import javax.inject.Inject

/**
 * Informe pericial en PDF (US21 E2, US43 E1, `iot:health_reports:read`). Queda en `cacheDir/pdfs`; abrirlo o
 * compartirlo con FileProvider es de la UI.
 */
class DownloadHealthReportPdfUseCase @Inject constructor(
    private val healthReportRepository: HealthReportRepository
) {
    suspend operator fun invoke(reportId: String): Result<File> = healthReportRepository.downloadPdf(reportId)
}
