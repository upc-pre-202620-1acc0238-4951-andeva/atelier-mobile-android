package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import javax.inject.Inject

/** Reconecta con el último adaptador ELM327 cuando la sesión en vivo cae a `LOST` (US16 E2, US18). */
class ReconnectObdUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    suspend operator fun invoke(): Result<ObdConnectionInfo> = session.reconnect()
}
