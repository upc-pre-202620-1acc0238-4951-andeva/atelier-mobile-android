package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySnapshot
import javax.inject.Inject

/** Congela la última muestra (US18 E2). Ya está guardada y viaja en el lote; no hay endpoint para adjuntarla a la orden. */
class FreezeTelemetryUseCase @Inject constructor(
    private val session: DiagnosticSession
) {
    operator fun invoke(): Result<TelemetrySnapshot> =
        session.freeze()?.let { Result.success(it) } ?: Result.failure(ObdNotConnectedException())
}
