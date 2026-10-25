package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import javax.inject.Inject

/**
 * Escáner instalado en el vehículo (US16). null = sin instalación: el lote de telemetría respondería 404, así que
 * conviene instalarlo antes de capturar. En línea.
 */
class GetActiveInstallationUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<DeviceInstallation?> =
        diagnosticsRepository.getActiveInstallation(vehicleId)
}
