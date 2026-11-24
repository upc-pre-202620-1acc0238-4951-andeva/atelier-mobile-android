package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import javax.inject.Inject

/**
 * Alertas predictivas activas de todo el taller (`GET /api/v1/iot/alerts/tenant`, permiso `iot:alerts:read`).
 * En línea; también actualiza la caché local `predictive_alerts`.
 */
class GetTenantPredictiveAlertsUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(): Result<List<PredictiveAlert>> =
        diagnosticsRepository.getTenantPredictiveAlerts()
}
