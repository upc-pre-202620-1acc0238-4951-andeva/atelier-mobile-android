package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import javax.inject.Inject

/** Recarga las alertas predictivas del vehículo (`iot:alerts:read`); no hay push de alertas al staff. */
class RefreshPredictiveAlertsUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<Unit> = diagnosticsRepository.refreshPredictiveAlerts(vehicleId)
}
