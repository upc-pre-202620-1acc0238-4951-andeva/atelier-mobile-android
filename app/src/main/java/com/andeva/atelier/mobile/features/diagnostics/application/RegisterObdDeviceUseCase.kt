package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import javax.inject.Inject

/**
 * Da de alta un nuevo escáner OBD-II en el inventario del taller (`POST /api/v1/iot/devices`, permiso `iot:devices:register`).
 * El backend solo admite `BLUETOOTH_BLE` en `ConnectionType` (H18, docs/03-traceability.md).
 */
class RegisterObdDeviceUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(
        deviceIdentifier: String,
        connectionType: String = ALLOWED_CONNECTION_TYPE,
        hardwareModel: String? = null,
        firmwareVersion: String? = null
    ): Result<ObdDevice> {
        val trimmedId = deviceIdentifier.trim()
        val trimmedType = connectionType.trim()
        if (trimmedId.isBlank()) {
            return Result.failure(InvalidDiagnosticsInputException("deviceIdentifier"))
        }
        if (trimmedType != ALLOWED_CONNECTION_TYPE) {
            return Result.failure(InvalidDiagnosticsInputException("connectionType"))
        }
        return diagnosticsRepository.registerDevice(
            deviceIdentifier = trimmedId,
            connectionType = trimmedType,
            hardwareModel = hardwareModel,
            firmwareVersion = firmwareVersion
        )
    }

    companion object {
        const val ALLOWED_CONNECTION_TYPE = "BLUETOOTH_BLE"
    }
}
