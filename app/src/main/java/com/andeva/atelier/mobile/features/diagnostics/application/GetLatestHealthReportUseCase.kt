package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import javax.inject.Inject

/** Último informe del vehículo sin volver a llamar a la IA (US21). null = el vehículo no tiene informes. */
class GetLatestHealthReportUseCase @Inject constructor(
    private val healthReportRepository: HealthReportRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<HealthReport?> = healthReportRepository.getLatest(vehicleId)
}
