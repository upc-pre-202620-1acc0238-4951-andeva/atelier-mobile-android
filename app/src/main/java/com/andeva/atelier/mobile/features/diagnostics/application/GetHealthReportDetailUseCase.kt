package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportDetail
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import javax.inject.Inject

/** Diagnóstico completo (subsistemas, riesgos, servicios sugeridos y correlaciones DTC) para validarlo (US21 E1). */
class GetHealthReportDetailUseCase @Inject constructor(
    private val healthReportRepository: HealthReportRepository
) {
    suspend operator fun invoke(reportId: String): Result<HealthReportDetail> = healthReportRepository.getDetail(reportId)
}
