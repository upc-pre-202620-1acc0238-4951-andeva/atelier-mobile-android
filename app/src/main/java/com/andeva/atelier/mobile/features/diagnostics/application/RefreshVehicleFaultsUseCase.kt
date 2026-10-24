package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import javax.inject.Inject

/** Recarga las fallas activas del vehículo (US17); sin red se sigue con la caché. */
class RefreshVehicleFaultsUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<Unit> = diagnosticsRepository.refreshActiveFaults(vehicleId)
}
