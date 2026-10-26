package com.andeva.atelier.mobile.features.reports.application

import com.andeva.atelier.mobile.features.reports.domain.InvalidReportInputException
import com.andeva.atelier.mobile.features.reports.domain.ReportsRepository
import java.io.File
import javax.inject.Inject

/** PDF de salud de la flota de un cliente; la UI lo habilita con `Session.canReadAiReports()`. */
class DownloadFleetHealthPdfUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    suspend operator fun invoke(customerId: String): Result<File> {
        if (customerId.isBlank()) return Result.failure(InvalidReportInputException("customerId"))
        return reportsRepository.downloadFleetHealthPdf(customerId)
    }
}
