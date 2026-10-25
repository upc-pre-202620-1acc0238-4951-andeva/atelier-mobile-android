package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * `telemetry_readings` no es caché sino captura (docs/06-offline-sync.md §6): cada fila espera su `TELEMETRY_BATCH`.
 * Una lectura está confirmada cuando su lote ya salió de `pending_operations`.
 */
@Dao
interface TelemetryReadingDao {
    /** Única por vehículo + `timestamp`: la misma muestra no se guarda dos veces. Devuelve -1 si ya estaba. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reading: TelemetryReadingEntity): Long

    @Query("SELECT DISTINCT vehicle_id FROM telemetry_readings WHERE batch_operation_id IS NULL")
    suspend fun vehiclesWithUnbatched(): List<String>

    @Query(
        "SELECT * FROM telemetry_readings WHERE vehicle_id = :vehicleId AND batch_operation_id IS NULL " +
            "ORDER BY timestamp LIMIT :limit"
    )
    suspend fun unbatched(vehicleId: String, limit: Int): List<TelemetryReadingEntity>

    @Query("UPDATE telemetry_readings SET batch_operation_id = :operationId WHERE id IN (:ids)")
    suspend fun assignBatch(ids: List<String>, operationId: String)

    /** Lecturas del vehículo que aún no llegan al backend: sin lote o con el lote todavía en cola (US19 E1). */
    @Query(
        "SELECT COUNT(*) FROM telemetry_readings WHERE vehicle_id = :vehicleId AND (batch_operation_id IS NULL " +
            "OR batch_operation_id IN (SELECT id FROM pending_operations))"
    )
    fun observePendingCount(vehicleId: String): Flow<Int>

    /** Confirmadas (su lote ya no está en cola) y con más de 7 días: el histórico lo tiene el backend. */
    @Query(
        "DELETE FROM telemetry_readings WHERE batch_operation_id IS NOT NULL " +
            "AND batch_operation_id NOT IN (SELECT id FROM pending_operations) AND timestamp < :before"
    )
    suspend fun purgeConfirmed(before: Long): Int

    @Query("SELECT * FROM telemetry_readings WHERE vehicle_id = :vehicleId ORDER BY timestamp")
    suspend fun findForVehicle(vehicleId: String): List<TelemetryReadingEntity>
}
