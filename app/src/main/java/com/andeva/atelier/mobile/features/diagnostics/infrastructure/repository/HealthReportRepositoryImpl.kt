package com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository

import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportDetail
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.GenerateHealthReportRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import com.google.gson.Gson
import java.io.File
import javax.inject.Inject

/** Informes de salud con IA: todo en línea, sin caché (docs/06-offline-sync.md §2). */
class HealthReportRepositoryImpl @Inject constructor(
    private val iotService: IotService,
    private val pdfDownloader: PdfDownloader,
    private val gson: Gson
) : HealthReportRepository {

    override suspend fun generate(vehicleId: String, daysToAnalyze: Int, includeResolvedDtcHistory: Boolean?): Result<HealthReport> =
        apiCall(gson, {
            iotService.generateHealthReport(vehicleId, GenerateHealthReportRequestDto(daysToAnalyze, includeResolvedDtcHistory))
        }) { it.toDomain(vehicleId) }

    override suspend fun getLatest(vehicleId: String): Result<HealthReport?> {
        val result = apiCall(gson, { iotService.getLatestHealthReport(vehicleId) }) { it.toDomain(vehicleId) }
        val error = result.exceptionOrNull()
        return if (error is ApiException && error.httpStatus == 404) Result.success(null) else result
    }

    override suspend fun getDetail(reportId: String): Result<HealthReportDetail> =
        apiCall(gson, { iotService.getHealthReport(reportId) }) { it.toDomain() }

    override suspend fun downloadPdf(reportId: String): Result<File> =
        pdfDownloader.download("informe-salud-$reportId") { iotService.getHealthReportPdf(reportId) }
}
