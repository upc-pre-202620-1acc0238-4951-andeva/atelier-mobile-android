package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import javax.inject.Inject

/**
 * Escáneres registrados en el taller (US16, `iot:devices:read`), para elegir cuál instalar. `ObdDevice.matches`
 * sugiere el que corresponde al adaptador emparejado, sin darlo por hecho. En línea.
 */
class GetObdDevicesUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(status: DeviceStatus? = DeviceStatus.ACTIVE): Result<List<ObdDevice>> =
        diagnosticsRepository.getDevices(status)
}
