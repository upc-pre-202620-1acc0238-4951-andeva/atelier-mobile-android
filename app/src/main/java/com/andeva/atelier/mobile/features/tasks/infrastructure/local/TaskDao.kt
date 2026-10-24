package com.andeva.atelier.mobile.features.tasks.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Una fila de `tasks` con la placa (`vehicles`) y el nombre de la bahía (`bays`) si ya están en caché. */
data class TaskWithRefs(
    @Embedded val task: TaskEntity,
    @ColumnInfo(name = "plate") val plate: String?,
    @ColumnInfo(name = "bay_name") val bayName: String?
)

@Dao
abstract class TaskDao {
    @Upsert
    protected abstract suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks")
    protected abstract suspend fun findAll(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    protected abstract suspend fun deleteAll()

    /**
     * Recarga completa del alcance `tasks:my` (el backend no soporta deltas, docs/06-offline-sync.md §4).
     * `AssignedTaskResource` no trae servicio, mecánico ni motivo de pausa: se conservan los del detalle ya
     * guardado, y los de pausa o suspensión solo si el estado no cambió (si cambió ya no valen).
     */
    @Transaction
    open suspend fun replaceMine(tasks: List<TaskEntity>) {
        val previous = findAll().associateBy { it.id }
        deleteAll()
        upsertAll(tasks.map { row -> previous[row.id]?.let { row.keepDetailOf(it) } ?: row })
    }

    /** Lo que trae `WorkOrderTaskResource` (sin vehículo, sede, bahía ni número de orden). Devuelve las filas tocadas. */
    @Query(
        "UPDATE tasks SET status = :status, description = :description, service_name = :serviceName, " +
            "mechanic_id = :mechanicId, hold_reason = :holdReason, missing_item_description = :missingItemDescription, " +
            "pending_suspension_reason = :pendingSuspensionReason, started_at = :startedAt, " +
            "total_paused_seconds = :totalPausedSeconds, synced_at = :syncedAt WHERE id = :id"
    )
    abstract suspend fun mergeDetail(
        id: String,
        status: String,
        description: String,
        serviceName: String?,
        mechanicId: String?,
        holdReason: String?,
        missingItemDescription: String?,
        pendingSuspensionReason: String?,
        startedAt: Long?,
        totalPausedSeconds: Long,
        syncedAt: Long
    ): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract suspend fun findById(id: String): TaskEntity?

    /** En curso → en pausa o suspendidas → el resto, y por número de orden. */
    @Query(
        "SELECT t.*, v.plate AS plate, b.name AS bay_name FROM tasks t " +
            "LEFT JOIN vehicles v ON v.id = t.vehicle_id LEFT JOIN bays b ON b.id = t.bay_id " +
            "ORDER BY CASE t.status WHEN 'IN_PROGRESS' THEN 0 WHEN 'ON_HOLD' THEN 1 ELSE 2 END, t.work_order_number"
    )
    abstract fun observeAll(): Flow<List<TaskWithRefs>>

    @Query(
        "SELECT t.*, v.plate AS plate, b.name AS bay_name FROM tasks t " +
            "LEFT JOIN vehicles v ON v.id = t.vehicle_id LEFT JOIN bays b ON b.id = t.bay_id WHERE t.id = :id"
    )
    abstract fun observeById(id: String): Flow<TaskWithRefs?>
}

private fun TaskEntity.keepDetailOf(previous: TaskEntity): TaskEntity {
    val sameStatus = status == previous.status
    return copy(
        serviceName = serviceName ?: previous.serviceName,
        mechanicId = mechanicId ?: previous.mechanicId,
        holdReason = holdReason ?: previous.holdReason.takeIf { sameStatus },
        missingItemDescription = missingItemDescription ?: previous.missingItemDescription.takeIf { sameStatus },
        pendingSuspensionReason = pendingSuspensionReason ?: previous.pendingSuspensionReason.takeIf { sameStatus }
    )
}
