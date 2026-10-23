package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    /** Respeta `dedupe_key`: si el mismo hecho ya está en cola devuelve -1 y no se duplica. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(operation: PendingOperationEntity): Long

    @Query(
        "SELECT * FROM pending_operations WHERE user_id = :userId AND status = 'PENDING' " +
            "AND next_attempt_at <= :now ORDER BY created_at"
    )
    suspend fun nextBatch(userId: String, now: Long): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE id = :id")
    suspend fun findById(id: String): PendingOperationEntity?

    /** Próximo `next_attempt_at` de lo que sigue PENDING, para reprogramar el worker tras el backoff. */
    @Query("SELECT MIN(next_attempt_at) FROM pending_operations WHERE user_id = :userId AND status = 'PENDING'")
    suspend fun nextAttemptAt(userId: String): Long?

    @Query("UPDATE pending_operations SET status = 'SYNCING' WHERE id = :id")
    suspend fun markSyncing(id: String)

    @Query(
        "UPDATE pending_operations SET status = 'PENDING', attempts = :attempts, next_attempt_at = :nextAttemptAt, " +
            "last_http_status = :httpStatus, last_error_code = :code, last_error_message = :message WHERE id = :id"
    )
    suspend fun markRetry(id: String, attempts: Int, nextAttemptAt: Long, httpStatus: Int?, code: String?, message: String?)

    @Query(
        "UPDATE pending_operations SET status = 'FAILED', attempts = :attempts, " +
            "last_http_status = :httpStatus, last_error_code = :code, last_error_message = :message WHERE id = :id"
    )
    suspend fun markFailed(id: String, attempts: Int, httpStatus: Int?, code: String?, message: String?)

    /** Las que dependen de una operación fallida no deben esperar para siempre. Devuelve sus ids. */
    @Query("SELECT id FROM pending_operations WHERE depends_on = :parentId AND status != 'FAILED'")
    suspend fun dependentIds(parentId: String): List<String>

    @Query("UPDATE pending_operations SET status = 'FAILED', last_error_code = :code, last_error_message = :message WHERE id = :id")
    suspend fun failAsDependent(id: String, code: String?, message: String?)

    /** Operaciones que quedaron en SYNCING si el proceso murió a mitad del envío. */
    @Query("UPDATE pending_operations SET status = 'PENDING' WHERE user_id = :userId AND status = 'SYNCING'")
    suspend fun resetSyncing(userId: String)

    /** Acción "Reintentar" de la pantalla de pendientes. */
    @Query("UPDATE pending_operations SET status = 'PENDING', attempts = 0, next_attempt_at = :now WHERE id = :id")
    suspend fun retry(id: String, now: Long)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM pending_operations WHERE user_id = :userId")
    fun countPending(userId: String): Flow<Int>

    @Query("SELECT * FROM pending_operations WHERE user_id = :userId ORDER BY created_at")
    fun observeForUser(userId: String): Flow<List<PendingOperationEntity>>

    /** Solo si el usuario elige descartar al cerrar sesión: la cola de otros usuarios nunca se borra. */
    @Query("DELETE FROM pending_operations WHERE user_id = :userId")
    suspend fun deleteForUser(userId: String)
}
