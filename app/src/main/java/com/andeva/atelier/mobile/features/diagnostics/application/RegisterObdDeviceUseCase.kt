package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import javax.inject.Inject

/**
 * Da de alta un nuevo escáner OBD-II en el inventario del taller (`POST /api/v1/iot/devices`, permiso `iot:devices:register`).
 */
class RegisterObdDeviceUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(
        deviceIdentifier: String,
        connectionType: String = "BLUETOOTH",
        hardwareModel: String? = null,
        firmwareVersion: String? = null
    ): Result<ObdDevice> {
        if (deviceIdentifier.isBlank() || connectionType.isBlank()) {
            return Result.failure(IllegalArgumentException("deviceIdentifier y connectionType son obligatorios"))
        }
        return diagnosticsRepository.registerDevice(
            deviceIdentifier = deviceIdentifier,
            connectionType = connectionType,
            hardwareModel = hardwareModel,
            firmwareVersion = firmwareVersion
        )
    }
}
