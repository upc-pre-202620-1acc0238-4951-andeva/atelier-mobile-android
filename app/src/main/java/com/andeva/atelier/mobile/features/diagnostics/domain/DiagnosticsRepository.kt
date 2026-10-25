package com.andeva.atelier.mobile.features.diagnostics.domain

import kotlinx.coroutines.flow.Flow

/**
 * Diagnóstico OBD-II (US16–US20). Instalación del escáner: en línea. Lecturas y fallas: se capturan sin red y salen
 * por la cola (`TELEMETRY_BATCH`, `REGISTER_FAULT`, `RESOLVE_FAULT`, docs/06-offline-sync.md §3). Las fallas activas y
 * las alertas predictivas se leen offline-first desde `vehicle_faults` y `predictive_alerts`.
 */
interface DiagnosticsRepository {
    suspend fun getDevices(status: DeviceStatus?): Result<List<ObdDevice>>

    /** null = el vehículo no tiene escáner instalado (404). */
    suspend fun getActiveInstallation(vehicleId: String): Result<DeviceInstallation?>

    suspend fun installDevice(deviceId: String, vehicleId: String, currentOdometerKm: Int?): Result<DeviceInstallation>

    suspend fun uninstallDevice(installationId: String, finalOdometerKm: Int?): Result<DeviceInstallation>

    /** Guarda la muestra si se puede enviar ([TelemetrySample.canBeSent]); devuelve si se guardó. */
    suspend fun saveReading(vehicleId: String, sample: TelemetrySample, latitude: Double?, longitude: Double?): Boolean

    /**
     * Arma `TELEMETRY_BATCH` de hasta 200 lecturas sin lote. Con [onlyFull] solo los lotes completos (durante el
     * muestreo); sin él, también el resto (al iniciar y al cerrar). Devuelve cuántos lotes encoló.
     */
    suspend fun enqueueUnbatchedReadings(onlyFull: Boolean): Int

    /** Borra las lecturas ya confirmadas por el backend con más de 7 días (docs/06-offline-sync.md §6). */
    suspend fun purgeConfirmedReadings()

    fun observeSyncState(vehicleId: String): Flow<DiagnosticsSyncState>

    /** `GET /iot/faults/vehicle/{id}/active` → caché; conserva las fallas aún en cola. */
    suspend fun refreshActiveFaults(vehicleId: String): Result<Unit>

    fun observeActiveFaults(vehicleId: String): Flow<List<VehicleFault>>

    /**
     * Registra los códigos leídos (US17 E1, US20 E2): los que ya están activos se devuelven tal cual; los nuevos quedan
     * como `local-{uuid}` con su `REGISTER_FAULT` en cola.
     */
    suspend fun registerDetectedFaults(vehicleId: String, dtcs: List<Dtc>, sessionId: String): List<VehicleFault>

    /**
     * Tras el modo 04 y la re-lectura (US20): las fallas activas que ya no aparecen se resuelven; las que siguen en
     * [remaining] quedan como persistentes.
     */
    suspend fun applyClearedFaults(vehicleId: String, remaining: List<Dtc>, sessionId: String): ClearFaultsResult

    suspend fun refreshPredictiveAlerts(vehicleId: String): Result<Unit>

    fun observePredictiveAlerts(vehicleId: String): Flow<List<PredictiveAlert>>

    suspend fun acknowledgePredictiveAlert(id: String): Result<PredictiveAlert>
}
