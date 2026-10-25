package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.ClearFaultsResult
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import javax.inject.Inject

/**
 * Borrado de códigos tras la reparación (US20, `iot:faults:resolve`): modo 04 y re-lectura con el modo 03. Las fallas
 * que ya no aparecen se resuelven (`RESOLVE_FAULT` en cola, E1); las que reaparecen quedan en `persistent` (E2). Las
 * que aún no llegaban al backend se descartan.
 */
class ClearFaultsUseCase @Inject constructor(
    private val session: DiagnosticSession,
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<ClearFaultsResult> {
        val sessionId = session.sessionId ?: return Result.failure(ObdNotConnectedException())
        val remaining = session.withLink { link -> link.clearDtcs().mapCatching { link.readDtcs().getOrThrow() } }
        return remaining.map { diagnosticsRepository.applyClearedFaults(vehicleId, it, sessionId) }
    }
}
