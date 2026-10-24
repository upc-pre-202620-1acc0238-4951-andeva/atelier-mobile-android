package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(upload: PendingUploadEntity)

    @Query("SELECT * FROM pending_uploads WHERE id = :id")
    suspend fun findById(id: String): PendingUploadEntity?

    /** Fotos aún sin subir de un destino (`INTAKE`, `TASK_EVIDENCE`, `PROPOSAL`), para mostrar su archivo local. */
    @Query("SELECT * FROM pending_uploads WHERE target = :target")
    fun observeByTarget(target: String): Flow<List<PendingUploadEntity>>

    @Query("UPDATE pending_uploads SET public_url = :publicUrl WHERE id = :id")
    suspend fun setPublicUrl(id: String, publicUrl: String)

    @Query("DELETE FROM pending_uploads WHERE id = :id")
    suspend fun delete(id: String)
}
