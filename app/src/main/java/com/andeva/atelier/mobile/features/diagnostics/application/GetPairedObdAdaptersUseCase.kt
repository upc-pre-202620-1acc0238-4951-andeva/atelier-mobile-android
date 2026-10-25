package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapter
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterProvider
import javax.inject.Inject

/**
 * Adaptadores ELM327 emparejados en el teléfono (US16). Falla con `BluetoothPermissionDeniedException` (pedir
 * `BLUETOOTH_CONNECT` es de la UI) o `BluetoothDisabledException`.
 */
class GetPairedObdAdaptersUseCase @Inject constructor(
    private val adapterProvider: ObdAdapterProvider
) {
    operator fun invoke(): Result<List<ObdAdapter>> = adapterProvider.pairedAdapters()
}
