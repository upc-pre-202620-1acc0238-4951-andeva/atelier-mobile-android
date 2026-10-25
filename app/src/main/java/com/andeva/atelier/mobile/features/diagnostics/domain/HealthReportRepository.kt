package com.andeva.atelier.mobile.features.diagnostics.domain

import java.io.File

/** Informes de salud con IA (US21, US43 E1). Todo en línea: inferencia en el servidor y cuota mensual (06 §2). */
interface HealthReportRepository {
    /** `POST /iot/health-reports/generate` (síncrono): el resumen; el detalle se pide con [getDetail]. */
    suspend fun generate(vehicleId: String, daysToAnalyze: Int, includeResolvedDtcHistory: Boolean?): Result<HealthReport>

    /** null = el vehículo no tiene informes (404). */
    suspend fun getLatest(vehicleId: String): Result<HealthReport?>

    suspend fun getDetail(reportId: String): Result<HealthReportDetail>

    /** PDF en `cacheDir/pdfs`; abrirlo o compartirlo es de la UI. */
    suspend fun downloadPdf(reportId: String): Result<File>
}
