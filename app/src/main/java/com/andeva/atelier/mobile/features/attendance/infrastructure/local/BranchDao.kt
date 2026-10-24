package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BranchDao {
    @Upsert
    protected abstract suspend fun upsertAll(branches: List<BranchEntity>)

    @Query("DELETE FROM branches")
    protected abstract suspend fun deleteAll()

    /** Alcance "todo el taller" (docs/06-offline-sync.md §4). */
    @Transaction
    open suspend fun replaceAll(branches: List<BranchEntity>) {
        deleteAll()
        upsertAll(branches)
    }

    @Query("SELECT * FROM branches WHERE id = :id")
    abstract suspend fun findById(id: String): BranchEntity?

    @Query("SELECT * FROM branches WHERE id = :id")
    abstract fun observeById(id: String): Flow<BranchEntity?>
}
