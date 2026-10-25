package com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository

/** `dedupe_key` de la cola de diagnóstico (docs/06-offline-sync.md §3 y §6). */
object DiagnosticsDedupeKeys {
    /** Un mismo código no se encola dos veces por sesión de diagnóstico. */
    fun registerFault(vehicleId: String, dtcCode: String, sessionId: String) = "FAULT:$vehicleId:$dtcCode:$sessionId"

    const val RESOLVE_FAULT_PREFIX = "RESOLVE_FAULT:"

    fun resolveFault(faultId: String) = "$RESOLVE_FAULT_PREFIX$faultId"

    /** El mismo tramo de lecturas no forma dos lotes. */
    fun telemetryBatch(vehicleId: String, firstTimestamp: Long, lastTimestamp: Long) = "TELEMETRY:$vehicleId:$firstTimestamp:$lastTimestamp"
}
