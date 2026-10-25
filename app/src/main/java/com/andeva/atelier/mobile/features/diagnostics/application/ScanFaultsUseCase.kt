package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.FaultScanResult
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import javax.inject.Inject

/**
 * Escaneo de códigos de falla (US17): modo 03 y, si hay códigos, el cuadro congelado (modo 02). Cada código nuevo queda
 * como falla `local-…` con su `REGISTER_FAULT` en cola, con o sin red. Sin códigos: `isClean` (E2).
 */
class ScanFaultsUseCase @Inject constructor(
    private val session: DiagnosticSession,
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<FaultScanResult> {
        val sessionId = session.sessionId ?: return Result.failure(ObdNotConnectedException())
        val read = session.withLink { link ->
            link.readDtcs().map { dtcs ->
                val freezeFrame = if (dtcs.isEmpty()) null else link.readFreezeFrame().getOrNull()
                dtcs to freezeFrame
            }
        }
        return read.map { (dtcs, freezeFrame) ->
            FaultScanResult(diagnosticsRepository.registerDetectedFaults(vehicleId, dtcs, sessionId), freezeFrame)
        }
    }
}
