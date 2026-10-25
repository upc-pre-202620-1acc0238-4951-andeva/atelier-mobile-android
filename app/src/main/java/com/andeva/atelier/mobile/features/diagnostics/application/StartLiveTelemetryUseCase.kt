package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import javax.inject.Inject

/**
 * Lectura en vivo del vehículo cada 2 s (US18 E1). Las muestras se guardan sin red y salen en lotes de 200 (US19).
 * Sin conexión: `ObdNotConnectedException`.
 */
class StartLiveTelemetryUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    suspend operator fun invoke(vehicleId: String): Result<Unit> = session.startStreaming(vehicleId)
}
