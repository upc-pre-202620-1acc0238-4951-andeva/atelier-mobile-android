package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.LiveTelemetry
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Estado del enlace, última muestra y avisos por umbral (US18 E1; US16 E2 con `RECONNECTING` / `LOST`). */
class ObserveLiveTelemetryUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    operator fun invoke(): StateFlow<LiveTelemetry> = session.state
}
