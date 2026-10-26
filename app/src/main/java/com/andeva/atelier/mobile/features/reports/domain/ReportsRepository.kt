package com.andeva.atelier.mobile.features.reports.domain

import java.io.File

/** Contrato de reportes: flujo de caja y salud de flota. Solo en línea, sin caché. */
interface ReportsRepository {
    suspend fun getCashFlow(period: ReportPeriod): Result<CashFlowSummary>

    suspend fun downloadCashFlowPdf(period: ReportPeriod): Result<File>

    /** Informe consolidado de la flota del cliente; 404 si el cliente es de otro taller. */
    suspend fun downloadFleetHealthPdf(customerId: String): Result<File>
}
