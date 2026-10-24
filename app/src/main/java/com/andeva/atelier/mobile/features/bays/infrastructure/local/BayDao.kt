package com.andeva.atelier.mobile.features.bays.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Caché `bays` por sede (docs/06-offline-sync.md §4). Por ahora la llena `tasks` para mostrar el nombre
 * de la bahía (US26); el paso 9 (`bays`) la usa para el tablero.
 */
@Dao
abstract class BayDao {
    @Upsert
    protected abstract suspend fun upsertAll(bays: List<BayEntity>)

    @Query("DELETE FROM bays WHERE branch_id = :branchId")
    protected abstract suspend fun deleteBranch(branchId: String)

    /** Recarga completa del alcance `bays:{branchId}`. */
    @Transaction
    open suspend fun replaceBranch(branchId: String, bays: List<BayEntity>) {
        deleteBranch(branchId)
        upsertAll(bays)
    }

    @Query("SELECT * FROM bays WHERE id = :id")
    abstract suspend fun findById(id: String): BayEntity?

    @Query("SELECT * FROM bays WHERE id = :id")
    abstract fun observeById(id: String): Flow<BayEntity?>
}
