package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsValidation
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.InsufficientTelemetryException
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import javax.inject.Inject

/**
 * Dictamen predictivo con IA sobre los últimos [daysToAnalyze] días (US21 E1, `iot:health_reports:generate` y plan con
 * IA). 422 → [InsufficientTelemetryException] (E3); 403 (cuota del plan) y 502 (IA no disponible) llegan como
 * `ApiException` con el `message`. En línea. El detalle se pide con `GetHealthReportDetailUseCase`.
 */
class GenerateHealthReportUseCase @Inject constructor(
    private val healthReportRepository: HealthReportRepository
) {
    suspend operator fun invoke(vehicleId: String, daysToAnalyze: Int, includeResolvedDtcHistory: Boolean? = null): Result<HealthReport> {
        if (!DiagnosticsValidation.isDaysToAnalyze(daysToAnalyze)) return Result.failure(InvalidDiagnosticsInputException("daysToAnalyze"))
        val result = healthReportRepository.generate(vehicleId, daysToAnalyze, includeResolvedDtcHistory)
        val error = result.exceptionOrNull()
        return if (error is ApiException && error.httpStatus == 422) Result.failure(InsufficientTelemetryException(error.message)) else result
    }
}
