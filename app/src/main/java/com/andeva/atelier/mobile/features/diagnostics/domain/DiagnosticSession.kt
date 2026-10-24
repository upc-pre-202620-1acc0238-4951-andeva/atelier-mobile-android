package com.andeva.atelier.mobile.features.diagnostics.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Sesión de diagnóstico en bahía: dueña del [ObdLink] mientras dure (un solo canal serie, así que el muestreo en vivo
 * y los escaneos se turnan). Las lecturas se guardan en Room y salen en lotes por la cola (docs/06-offline-sync.md §6).
 */
interface DiagnosticSession {
    val state: StateFlow<LiveTelemetry>

    /** Id de la conexión actual, para la `dedupe_key` de `REGISTER_FAULT`; null sin conexión. */
    val sessionId: String?

    suspend fun connect(address: String): Result<ObdConnectionInfo>

    /** Muestreo cada 2 s del vehículo [vehicleId] (US18 E1, US19 E1). */
    suspend fun startStreaming(vehicleId: String): Result<Unit>

    /** Detiene el muestreo y encola lo que falte enviar. */
    suspend fun stopStreaming()

    suspend fun disconnect()

    /** La última muestra con sus avisos (US18 E2); null si aún no hay muestra. */
    fun freeze(): TelemetrySnapshot?

    /** Usa el canal en exclusiva (pausa el muestreo mientras dure). Sin conexión: [ObdNotConnectedException]. */
    suspend fun <T> withLink(block: suspend (ObdLink) -> Result<T>): Result<T>
}
