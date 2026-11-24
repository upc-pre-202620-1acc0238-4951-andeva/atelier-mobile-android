package com.andeva.atelier.mobile.features.diagnostics.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Diagnóstico OBD-II (US16–US20). Instalación del escáner: en línea. Lecturas y fallas: se capturan sin red y salen
 * por la cola (`TELEMETRY_BATCH`, `REGISTER_FAULT`, `RESOLVE_FAULT`, docs/06-offline-sync.md §3). Las fallas activas y
 * las alertas predictivas se leen offline-first desde `vehicle_faults` y `predictive_alerts`.
 */
interface DiagnosticsRepository {
    suspend fun getDevices(status: DeviceStatus?): Result<List<ObdDevice>>

    suspend fun registerDevice(
        deviceIdentifier: String,
        connectionType: String,
        hardwareModel: String?,
        firmwareVersion: String?
    ): Result<ObdDevice>

    suspend fun updateDeviceStatus(deviceId: String, status: DeviceStatus): Result<ObdDevice>

    /** null = el vehículo no tiene escáner instalado (404). */
    suspend fun getActiveInstallation(vehicleId: String): Result<DeviceInstallation?>

    suspend fun installDevice(deviceId: String, vehicleId: String, currentOdometerKm: Int?): Result<DeviceInstallation>

    suspend fun uninstallDevice(installationId: String, finalOdometerKm: Int?): Result<DeviceInstallation>

    /** Guarda la muestra si se puede enviar ([TelemetrySample.canBeSent]); devuelve si se guardó. */
    suspend fun saveReading(vehicleId: String, sample: TelemetrySample, latitude: Double?, longitude: Double?): Boolean

    /**
     * Arma `TELEMETRY_BATCH` de hasta 200 lecturas sin lote. Con [onlyFull] solo los lotes completos (durante el
     * muestreo); sin él, también el resto (al iniciar, al cerrar y en el pre-sync del worker).
     * [userId] aísla las lecturas del técnico propietario; [excludeVehicleId] omite un vehículo en muestreo activo y
     * [minIdleMs] exige que hayan pasado al menos esos ms desde la última muestra para empaquetar remanentes `< 200`.
     */
    suspend fun enqueueUnbatchedReadings(
        onlyFull: Boolean,
        userId: String? = null,
        excludeVehicleId: String? = null,
        minIdleMs: Long = 0L
    ): Int

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

    /** `GET /iot/alerts/tenant` (`iot:alerts:read`): alertas activas del taller. */
    suspend fun getTenantPredictiveAlerts(): Result<List<PredictiveAlert>>

    fun observeAllPredictiveAlerts(): Flow<List<PredictiveAlert>>

    /** `POST /iot/alerts/{id}/convert-to-appointment` (`crm:appointments:create`). */
    suspend fun convertAlertToAppointment(
        alertId: String,
        scheduledAt: Instant? = null,
        estimatedDurationMinutes: Int? = null,
        reason: String? = null
    ): Result<ConvertedAppointment>

    /** `GET /iot/telemetry/vehicle/{vehicleId}/latest` (`iot:telemetry:read`); null si 404. */
    suspend fun getLatestVehicleTelemetry(vehicleId: String): Result<LatestVehicleTelemetry?>

    /** `GET /iot/telemetry/vehicle/{vehicleId}/history` (`iot:telemetry:read`). */
    suspend fun getVehicleTelemetryHistory(
        vehicleId: String,
        from: Instant? = null,
        to: Instant? = null,
        bucket: String? = null
    ): Result<List<TelemetryHistoryBucket>>
}
