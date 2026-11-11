package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import javax.inject.Inject

/**
 * Actualiza el estado operativo de un escáner OBD-II (`PATCH /api/v1/iot/devices/{id}/status`, permiso `iot:devices:register`).
 */
class UpdateObdDeviceStatusUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(deviceId: String, status: DeviceStatus): Result<ObdDevice> =
        diagnosticsRepository.updateDeviceStatus(deviceId, status)
}
