package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import javax.inject.Inject

/** Detiene la lectura en vivo y encola lo capturado; la conexión sigue abierta para escanear o borrar códigos. */
class StopLiveTelemetryUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    suspend operator fun invoke() = session.stopStreaming()
}
