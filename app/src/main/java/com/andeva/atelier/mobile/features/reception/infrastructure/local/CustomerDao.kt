package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Caché `customers` (docs/06-offline-sync.md §4, clientes vistos o buscados · 7 días). */
@Dao
interface CustomerDao {
    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Upsert
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun findById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: String): Flow<CustomerEntity?>

    /** Búsqueda sin red: documento exacto al inicio o parte del nombre. */
    @Query(
        "SELECT * FROM customers WHERE tax_id LIKE :query || '%' OR display_name LIKE '%' || :query || '%' " +
            "ORDER BY display_name LIMIT 50"
    )
    suspend fun search(query: String): List<CustomerEntity>
}
