package com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.domain.ClearFaultsResult
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.DtcCatalog
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.PredictiveAlertDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.TelemetryReadingDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.TelemetryReadingEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.toItemDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.AcknowledgeAlertRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.InstallDeviceRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.RegisterFaultRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.ResolveFaultPayload
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.TelemetryBatchRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.UninstallDeviceRequestDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.RegisterFaultHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.ResolveFaultHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.TelemetryBatchHandler
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Instalación del escáner en línea; lecturas y fallas capturadas sin red y enviadas por la cola
 * (docs/06-offline-sync.md §6, "US16–US19 — OBD-II en foso"); fallas activas y alertas offline-first.
 */
class DiagnosticsRepositoryImpl @Inject constructor(
    private val iotService: IotService,
    private val readingDao: TelemetryReadingDao,
    private val faultDao: VehicleFaultDao,
    private val alertDao: PredictiveAlertDao,
    private val pendingOperationDao: PendingOperationDao,
    private val queue: PendingOperationQueue,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : DiagnosticsRepository {

    override suspend fun getDevices(status: DeviceStatus?): Result<List<ObdDevice>> =
        apiCall(gson, { iotService.getDevices(status?.name) }) { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getActiveInstallation(vehicleId: String): Result<DeviceInstallation?> {
        val result = apiCall(gson, { iotService.getActiveInstallation(vehicleId) }) { it.toDomain() }
        val error = result.exceptionOrNull()
        return if (error is ApiException && error.httpStatus == 404) Result.success(null) else result
    }

    override suspend fun installDevice(deviceId: String, vehicleId: String, currentOdometerKm: Int?): Result<DeviceInstallation> =
        apiCall(gson, { iotService.install(InstallDeviceRequestDto(deviceId, vehicleId, currentOdometerKm)) }) { it.toDomain() }

    override suspend fun uninstallDevice(installationId: String, finalOdometerKm: Int?): Result<DeviceInstallation> =
        apiCall(gson, { iotService.uninstall(installationId, UninstallDeviceRequestDto(finalOdometerKm)) }) { it.toDomain() }

    override suspend fun saveReading(vehicleId: String, sample: TelemetrySample, latitude: Double?, longitude: Double?): Boolean {
        val engineTemp = sample.engineTempCelsius ?: return false
        val row = TelemetryReadingEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            timestamp = sample.timestamp.toEpochMilli(),
            engineTempCelsius = engineTemp,
            engineRpm = sample.engineRpm,
            speedKmh = sample.speedKmh,
            batteryVoltage = sample.batteryVoltage,
            fuelPercentage = sample.fuelPercentage,
            latitude = latitude,
            longitude = longitude
        )
        return readingDao.insert(row) != -1L
    }

    /**
     * Si el lote ya estaba en cola (se cortó entre encolar y marcar las filas) se reutiliza su operación por la
     * `dedupe_key`, así ninguna lectura queda sin lote ni viaja dos veces.
     */
    override suspend fun enqueueUnbatchedReadings(onlyFull: Boolean): Int {
        if (sessionManager.getSession() == null) return 0
        var batches = 0
        for (vehicleId in readingDao.vehiclesWithUnbatched()) {
            while (true) {
                val rows = readingDao.unbatched(vehicleId, BATCH_SIZE)
                if (rows.isEmpty() || (onlyFull && rows.size < BATCH_SIZE)) break
                val dedupeKey = DiagnosticsDedupeKeys.telemetryBatch(vehicleId, rows.first().timestamp, rows.last().timestamp)
                val body = TelemetryBatchRequestDto(vehicleId, rows.map { it.toItemDto() })
                val operationId = queue.enqueue(TelemetryBatchHandler.TYPE, body, dedupeKey, nowMillis = clock.millis())
                    ?: pendingOperationDao.findByDedupeKey(dedupeKey)?.id
                    ?: break
                readingDao.assignBatch(rows.map { it.id }, operationId)
                batches++
            }
        }
        return batches
    }

    override suspend fun purgeConfirmedReadings() {
        readingDao.purgeConfirmed(clock.millis() - READINGS_RETENTION_MILLIS)
    }

    override fun observeSyncState(vehicleId: String): Flow<DiagnosticsSyncState> =
        combine(readingDao.observePendingCount(vehicleId), faultDao.observePendingCount(vehicleId)) { readings, faults ->
            DiagnosticsSyncState(readings, faults)
        }

    /** Las fallas con un `RESOLVE_FAULT` en cola siguen activas en el backend, pero en la app ya están borradas. */
    override suspend fun refreshActiveFaults(vehicleId: String): Result<Unit> {
        if (sessionManager.getSession()?.has(Permission.FAULTS_READ) != true) return Result.success(Unit)
        val now = clock.millis()
        return apiCall(gson, { iotService.getActiveFaults(vehicleId) }) { dtos -> dtos.map { it.toEntity(vehicleId, now) } }
            .map { rows ->
                val resolving = pendingOperationDao
                    .observeByTypeAndDedupePrefix(ResolveFaultHandler.TYPE, DiagnosticsDedupeKeys.RESOLVE_FAULT_PREFIX).first()
                    .map { it.dedupeKey.removePrefix(DiagnosticsDedupeKeys.RESOLVE_FAULT_PREFIX) }
                    .toSet()
                faultDao.replaceRemote(vehicleId, rows.map { if (it.id in resolving) it.copy(isResolved = true, resolvedAt = now) else it })
            }
    }

    override fun observeActiveFaults(vehicleId: String): Flow<List<VehicleFault>> =
        faultDao.observeActive(vehicleId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun registerDetectedFaults(vehicleId: String, dtcs: List<Dtc>, sessionId: String): List<VehicleFault> {
        val active = faultDao.findActive(vehicleId)
        return dtcs.distinct().map { dtc ->
            active.firstOrNull { it.dtcCode == dtc.code }?.toDomain() ?: queueFault(vehicleId, dtc, sessionId)
        }
    }

    /**
     * La fila local se guarda antes de encolar: el worker puede enviar el registro de inmediato y debe encontrarla para
     * reemplazarla por la del backend.
     */
    private suspend fun queueFault(vehicleId: String, dtc: Dtc, sessionId: String): VehicleFault {
        val entry = DtcCatalog.describe(dtc)
        val operationId = UUID.randomUUID().toString()
        val now = clock.millis()
        val local = VehicleFaultEntity(
            id = VehicleFault.LOCAL_PREFIX + operationId,
            vehicleId = vehicleId,
            dtcCode = dtc.code,
            severity = entry.severity.name,
            description = entry.description,
            detectedAt = now,
            isResolved = false,
            resolvedAt = null,
            syncedAt = null
        )
        faultDao.upsert(local)
        val dedupeKey = DiagnosticsDedupeKeys.registerFault(vehicleId, dtc.code, sessionId)
        val body = RegisterFaultRequestDto(vehicleId, dtc.code, entry.severity.name, entry.description)
        if (queue.enqueue(RegisterFaultHandler.TYPE, body, dedupeKey, id = operationId, nowMillis = now) != null) {
            return local.toDomain()
        }
        // Ya estaba en cola en esta sesión: se devuelve esa falla.
        faultDao.deleteById(local.id)
        val existingId = pendingOperationDao.findByDedupeKey(dedupeKey)?.id
        return existingId?.let { faultDao.findById(VehicleFault.LOCAL_PREFIX + it)?.toDomain() } ?: local.toDomain()
    }

    override suspend fun applyClearedFaults(vehicleId: String, remaining: List<Dtc>, sessionId: String): ClearFaultsResult {
        val remainingCodes = remaining.map { it.code }.toSet()
        val now = clock.millis()
        val cleared = faultDao.findActive(vehicleId)
            .filter { it.dtcCode !in remainingCodes }
            .map { fault ->
                if (VehicleFault.isLocalId(fault.id)) discardLocal(fault, now) else queueResolve(fault, now, dependsOn = null)
                fault.toDomain().copy(isResolved = true, resolvedAt = Instant.ofEpochMilli(now))
            }
        return ClearFaultsResult(cleared, registerDetectedFaults(vehicleId, remaining, sessionId))
    }

    /**
     * Una falla que nunca llegó al backend se descarta junto con su `REGISTER_FAULT` (docs/06-offline-sync.md §6). Si
     * ese registro ya se está enviando, se resuelve después de él.
     */
    private suspend fun discardLocal(fault: VehicleFaultEntity, now: Long) {
        val operationId = fault.id.removePrefix(VehicleFault.LOCAL_PREFIX)
        val operation = pendingOperationDao.findById(operationId)
        if (operation?.status == SYNCING) {
            queueResolve(fault, now, dependsOn = operationId)
        } else {
            operation?.let { pendingOperationDao.delete(it.id) }
            faultDao.deleteById(fault.id)
        }
    }

    private suspend fun queueResolve(fault: VehicleFaultEntity, now: Long, dependsOn: String?) {
        faultDao.markResolved(fault.id, now)
        queue.enqueue(
            type = ResolveFaultHandler.TYPE,
            payload = ResolveFaultPayload(fault.id, fault.vehicleId, fault.dtcCode),
            dedupeKey = DiagnosticsDedupeKeys.resolveFault(fault.id),
            dependsOn = dependsOn,
            nowMillis = now
        )
    }

    override suspend fun refreshPredictiveAlerts(vehicleId: String): Result<Unit> {
        if (sessionManager.getSession()?.has(Permission.PREDICTIVE_ALERTS_READ) != true) return Result.success(Unit)
        val now = clock.millis()
        return apiCall(gson, { iotService.getVehicleAlerts(vehicleId) }) { dtos -> dtos.map { it.toEntity(vehicleId, now) } }
            .map { rows -> alertDao.replaceForVehicle(vehicleId, rows) }
    }

    override fun observePredictiveAlerts(vehicleId: String): Flow<List<PredictiveAlert>> =
        alertDao.observeForVehicle(vehicleId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun acknowledgePredictiveAlert(id: String): Result<PredictiveAlert> =
        apiCall(gson, { iotService.acknowledgeAlert(id, AcknowledgeAlertRequestDto()) }) { it }
            .map { dto ->
                val vehicleId = dto.vehicleId.orEmpty()
                if (vehicleId.isNotEmpty()) alertDao.upsert(dto.toEntity(vehicleId, clock.millis()))
                dto.toDomain(vehicleId)
            }

    companion object {
        /** Lecturas por `TELEMETRY_BATCH` (docs/04-mobile-integration.md §4). */
        const val BATCH_SIZE = 200
        const val READINGS_RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val SYNCING = "SYNCING"
    }
}
