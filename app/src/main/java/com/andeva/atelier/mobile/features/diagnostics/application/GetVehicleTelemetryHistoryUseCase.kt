package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryHistoryBucket
import java.time.Instant
import javax.inject.Inject

/**
 * Serie histórica agregada por `time_bucket` (`GET /api/v1/iot/telemetry/vehicle/{vehicleId}/history`,
 * permiso `iot:telemetry:read`). En línea.
 */
class GetVehicleTelemetryHistoryUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(
        vehicleId: String,
        from: Instant? = null,
        to: Instant? = null,
        bucket: String? = null
    ): Result<List<TelemetryHistoryBucket>> =
        diagnosticsRepository.getVehicleTelemetryHistory(vehicleId, from, to, bucket)
}
