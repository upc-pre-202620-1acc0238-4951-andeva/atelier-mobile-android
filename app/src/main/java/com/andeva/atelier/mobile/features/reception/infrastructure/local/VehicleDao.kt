package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Caché `vehicles` (docs/06-offline-sync.md §4, vehículos vistos · 7 días). Por ahora la llena `tasks`
 * para mostrar la placa (US26); el paso 6 (`reception`) la amplía con la búsqueda por placa.
 */
@Dao
interface VehicleDao {
    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun findById(id: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: String): Flow<VehicleEntity?>
}
