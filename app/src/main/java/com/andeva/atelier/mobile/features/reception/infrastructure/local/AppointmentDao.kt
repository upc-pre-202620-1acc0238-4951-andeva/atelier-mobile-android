package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Una fila de `appointments` con la placa (`vehicles`) y el nombre del cliente (`customers`) si ya están en caché. */
data class AppointmentWithRefs(
    @Embedded val appointment: AppointmentEntity,
    @ColumnInfo(name = "plate") val plate: String?,
    @ColumnInfo(name = "customer_name") val customerName: String?
)

/** Caché `appointments` (docs/06-offline-sync.md §4, hoy y mañana · 5 min) por sede y día de Lima. */
@Dao
abstract class AppointmentDao {
    @Upsert
    abstract suspend fun upsert(appointment: AppointmentEntity)

    @Upsert
    protected abstract suspend fun upsertAll(appointments: List<AppointmentEntity>)

    @Query("DELETE FROM appointments WHERE branch_id = :branchId AND scheduled_at >= :from AND scheduled_at < :to")
    protected abstract suspend fun deleteRange(branchId: String, from: Long, to: Long)

    /** Recarga completa del alcance `appointments:{branchId}:{fecha}`: [from]–[to] es el día de Lima en epoch ms. */
    @Transaction
    open suspend fun replaceDay(branchId: String, from: Long, to: Long, appointments: List<AppointmentEntity>) {
        deleteRange(branchId, from, to)
        upsertAll(appointments)
    }

    @Query("SELECT * FROM appointments WHERE id = :id")
    abstract suspend fun findById(id: String): AppointmentEntity?

    @Query(
        "SELECT a.*, v.plate AS plate, c.display_name AS customer_name FROM appointments a " +
            "LEFT JOIN vehicles v ON v.id = a.vehicle_id LEFT JOIN customers c ON c.id = a.customer_id " +
            "WHERE a.branch_id = :branchId AND a.scheduled_at >= :from AND a.scheduled_at < :to ORDER BY a.scheduled_at"
    )
    abstract fun observeDay(branchId: String, from: Long, to: Long): Flow<List<AppointmentWithRefs>>
}
