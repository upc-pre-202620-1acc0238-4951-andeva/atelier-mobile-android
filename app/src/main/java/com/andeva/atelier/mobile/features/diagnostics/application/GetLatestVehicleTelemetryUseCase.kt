package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.LatestVehicleTelemetry
import javax.inject.Inject

/**
 * Última lectura de telemetría remota del vehículo (`GET /api/v1/iot/telemetry/vehicle/{vehicleId}/latest`,
 * permiso `iot:telemetry:read`). Devuelve `null` si aún no hay lecturas (404). En línea.
 */
class GetLatestVehicleTelemetryUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<LatestVehicleTelemetry?> =
        diagnosticsRepository.getLatestVehicleTelemetry(vehicleId)
}
