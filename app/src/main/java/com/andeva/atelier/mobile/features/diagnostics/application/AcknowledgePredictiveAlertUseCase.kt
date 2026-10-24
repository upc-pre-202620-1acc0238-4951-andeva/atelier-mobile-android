package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import javax.inject.Inject

/** Marca la alerta como revisada (`iot:alerts:acknowledge`). En línea. */
class AcknowledgePredictiveAlertUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(id: String): Result<PredictiveAlert> = diagnosticsRepository.acknowledgePredictiveAlert(id)
}
