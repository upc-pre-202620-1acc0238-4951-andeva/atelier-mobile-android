package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsValidation
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import javax.inject.Inject

/** Retira el escáner del vehículo (US16, `iot:installations:manage`). En línea. */
class UninstallObdDeviceUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(installationId: String, finalOdometerKm: Int?): Result<DeviceInstallation> {
        if (!DiagnosticsValidation.isOdometer(finalOdometerKm)) return Result.failure(InvalidDiagnosticsInputException("finalOdometerKm"))
        return diagnosticsRepository.uninstallDevice(installationId, finalOdometerKm)
    }
}
