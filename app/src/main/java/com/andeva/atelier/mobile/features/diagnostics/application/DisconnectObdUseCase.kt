package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import javax.inject.Inject

/** Cierra la sesión: detiene el muestreo, encola las lecturas que falten (US19) y libera el adaptador. */
class DisconnectObdUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    suspend operator fun invoke() = session.disconnect()
}
