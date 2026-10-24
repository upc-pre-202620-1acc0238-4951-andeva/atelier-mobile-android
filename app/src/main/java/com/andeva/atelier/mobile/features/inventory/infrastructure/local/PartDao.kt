package com.andeva.atelier.mobile.features.inventory.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Caché `parts` (docs/06-offline-sync.md §4, catálogo · 1 h). Montos y cantidades se guardan como texto decimal:
 * las comparaciones de stock se hacen con `CAST(... AS REAL)` para no comparar texto (`"10" <= "9"`).
 */
@Dao
abstract class PartDao {
    @Upsert
    abstract suspend fun upsertAll(parts: List<PartEntity>)

    @Query("DELETE FROM parts")
    protected abstract suspend fun deleteAll()

    /** Recarga completa del alcance `parts:catalog` (se llama solo cuando llegaron todas las páginas). */
    @Transaction
    open suspend fun replaceAll(parts: List<PartEntity>) {
        deleteAll()
        upsertAll(parts)
    }

    @Query("SELECT * FROM parts WHERE id = :id")
    abstract suspend fun findById(id: String): PartEntity?

    @Query("SELECT * FROM parts WHERE id = :id")
    abstract fun observeById(id: String): Flow<PartEntity?>

    @Query("SELECT * FROM parts WHERE sku = :sku")
    abstract suspend fun findBySku(sku: String): PartEntity?

    @Query("SELECT * FROM parts WHERE (:category IS NULL OR category = :category) ORDER BY name")
    abstract fun observeAll(category: String?): Flow<List<PartEntity>>

    /** Búsqueda sin red, como la del backend: nombre o SKU, sin distinguir mayúsculas. */
    @Query(
        "SELECT * FROM parts WHERE (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%') " +
            "AND (:category IS NULL OR category = :category) ORDER BY name LIMIT 100"
    )
    abstract suspend fun search(query: String, category: String?): List<PartEntity>

    /** La misma regla de `GET /inventory/alerts` (H13): stock en el mínimo o debajo. */
    @Query("SELECT * FROM parts WHERE CAST(total_stock AS REAL) <= CAST(minimum_stock AS REAL) ORDER BY name")
    abstract suspend fun findLowStock(): List<PartEntity>

    @Query("SELECT * FROM parts WHERE CAST(total_stock AS REAL) <= CAST(minimum_stock AS REAL) ORDER BY name")
    abstract fun observeLowStock(): Flow<List<PartEntity>>
}
