package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import javax.inject.Inject

/**
 * Enlace con la computadora del vehículo (US16). Éxito: protocolo y VIN (E1). Fallos: `ObdNoResponseException`
 * (contacto apagado o sin alimentación, E2), `ObdAdapterUnreachableException`, `BluetoothPermissionDeniedException` o
 * `BluetoothDisabledException`. No necesita red.
 */
class ConnectObdUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    suspend operator fun invoke(adapterAddress: String): Result<ObdConnectionInfo> = session.connect(adapterAddress)
}
