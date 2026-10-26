package com.andeva.atelier.mobile.features.reports.application

import com.andeva.atelier.mobile.features.reports.domain.CashFlowSummary
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import com.andeva.atelier.mobile.features.reports.domain.ReportsRepository
import javax.inject.Inject

/** Flujo de caja del taller (todas las sedes) en un periodo; la UI lo habilita con `invoicing:cashflow:export_pdf`. */
class GetCashFlowSummaryUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    suspend operator fun invoke(period: ReportPeriod): Result<CashFlowSummary> = reportsRepository.getCashFlow(period)
}
