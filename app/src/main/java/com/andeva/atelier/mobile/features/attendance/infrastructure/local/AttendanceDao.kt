package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AttendanceDao {
    @Upsert
    abstract suspend fun upsert(attendance: AttendanceEntity)

    @Upsert
    abstract suspend fun upsertAll(attendances: List<AttendanceEntity>)

    @Query("DELETE FROM attendances WHERE membership_id = :membershipId AND clock_in >= :fromMillis AND clock_in < :toMillis")
    protected abstract suspend fun deleteRange(membershipId: String, fromMillis: Long, toMillis: Long)

    /** Recarga completa del alcance (el backend no soporta deltas, docs/06-offline-sync.md §4). */
    @Transaction
    open suspend fun replaceRange(membershipId: String, fromMillis: Long, toMillis: Long, attendances: List<AttendanceEntity>) {
        deleteRange(membershipId, fromMillis, toMillis)
        upsertAll(attendances)
    }

    /** La jornada más reciente: abierta (aunque haya empezado ayer) o la última cerrada. */
    @Query("SELECT * FROM attendances WHERE membership_id = :membershipId ORDER BY clock_in DESC LIMIT 1")
    abstract fun observeLatest(membershipId: String): Flow<AttendanceEntity?>

    @Query("SELECT * FROM attendances WHERE id = :id")
    abstract suspend fun findById(id: String): AttendanceEntity?

    @Query("SELECT * FROM attendances WHERE membership_id = :membershipId AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1")
    abstract suspend fun findOpen(membershipId: String): AttendanceEntity?

    @Query("SELECT * FROM attendances WHERE membership_id = :membershipId AND clock_in >= :fromMillis ORDER BY clock_in DESC")
    abstract fun observeSince(membershipId: String, fromMillis: Long): Flow<List<AttendanceEntity>>
}
