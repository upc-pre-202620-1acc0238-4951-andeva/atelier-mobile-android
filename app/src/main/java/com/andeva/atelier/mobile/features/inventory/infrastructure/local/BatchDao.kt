package com.andeva.atelier.mobile.features.inventory.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Caché `batches` por repuesto (docs/06-offline-sync.md §4, 1 h). Solo se llena con `inventory:batches:read`. */
@Dao
abstract class BatchDao {
    @Upsert
    protected abstract suspend fun upsertAll(batches: List<BatchEntity>)

    @Query("DELETE FROM batches WHERE item_id = :itemId")
    protected abstract suspend fun deleteForItem(itemId: String)

    /** Recarga completa del alcance `batches:{itemId}`. */
    @Transaction
    open suspend fun replaceForItem(itemId: String, batches: List<BatchEntity>) {
        deleteForItem(itemId)
        upsertAll(batches)
    }

    /** Orden FIFO (el más antiguo primero): el backend los manda del más nuevo al más viejo. */
    @Query("SELECT * FROM batches WHERE item_id = :itemId ORDER BY arrival_date ASC")
    abstract fun observeForItem(itemId: String): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE item_id = :itemId ORDER BY arrival_date ASC")
    abstract suspend fun findForItem(itemId: String): List<BatchEntity>
}
