package com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.sync.PreSyncHook
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import javax.inject.Inject

/**
 * Empaqueta lecturas `< 200` filas de `telemetry_readings` que quedaron sin lote tras una muerte de proceso
 * en fosa o al cerrar sesión (US19). Omite el vehículo que esté transmitiendo en vivo y exige 60 s de inactividad
 * salvo cuando [forceFlushAll] = true (`signOut`).
 */
class DiagnosticsPreSyncHook @Inject constructor(
    private val repository: DiagnosticsRepository,
    private val diagnosticSession: DiagnosticSession,
    private val sessionManager: SessionManager
) : PreSyncHook {

    override suspend fun beforeSync(userId: String, forceFlushAll: Boolean) {
        val session = sessionManager.getSession()
        if (session != null && !session.has(Permission.TELEMETRY_INGEST)) return
        repository.enqueueUnbatchedReadings(
            onlyFull = false,
            userId = userId,
            excludeVehicleId = if (forceFlushAll) null else diagnosticSession.activeVehicleId,
            minIdleMs = if (forceFlushAll) 0L else IDLE_WINDOW_MILLIS
        )
    }

    companion object {
        const val IDLE_WINDOW_MILLIS = 60_000L
    }
}
