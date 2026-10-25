package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsValidation
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import javax.inject.Inject

/**
 * Vincula el escáner al vehículo (US16 E1, `iot:installations:manage`). 409 si el escáner o el vehículo ya tienen una
 * instalación activa (`message`). En línea.
 */
class InstallObdDeviceUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(deviceId: String, vehicleId: String, currentOdometerKm: Int?): Result<DeviceInstallation> {
        if (!DiagnosticsValidation.isOdometer(currentOdometerKm)) return Result.failure(InvalidDiagnosticsInputException("currentOdometerKm"))
        return diagnosticsRepository.installDevice(deviceId, vehicleId, currentOdometerKm)
    }
}
