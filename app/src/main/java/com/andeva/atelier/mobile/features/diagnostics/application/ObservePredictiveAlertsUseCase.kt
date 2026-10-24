package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Alertas predictivas del vehículo desde Room, la más reciente primero. */
class ObservePredictiveAlertsUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    operator fun invoke(vehicleId: String): Flow<List<PredictiveAlert>> = diagnosticsRepository.observePredictiveAlerts(vehicleId)
}
