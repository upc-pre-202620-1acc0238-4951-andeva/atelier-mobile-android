package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Caché `vehicles` (docs/06-offline-sync.md §4, vehículos vistos · 7 días). La llenan `reception` (búsqueda por
 * placa, altas, citas) y `tasks` (placa de mis tareas, US26).
 */
@Dao
abstract class VehicleDao {
    @Upsert
    abstract suspend fun upsert(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE plate = :plate AND id != :id")
    protected abstract suspend fun deleteOtherWithPlate(plate: String, id: String)

    /**
     * Lo que vino del backend. La placa es única en la caché y `@Upsert` solo resuelve por id: si otra fila tiene
     * la misma placa (el backend la reasignó o la fila es vieja), se borra antes para no chocar con el índice.
     */
    @Transaction
    open suspend fun saveFromServer(vehicle: VehicleEntity) {
        deleteOtherWithPlate(vehicle.plate, vehicle.id)
        upsert(vehicle)
    }

    @Transaction
    open suspend fun saveAllFromServer(vehicles: List<VehicleEntity>) {
        vehicles.forEach { saveFromServer(it) }
    }

    @Query("SELECT * FROM vehicles WHERE id = :id")
    abstract suspend fun findById(id: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE plate = :plate")
    abstract suspend fun findByPlate(plate: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE id = :id")
    abstract fun observeById(id: String): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE current_owner_id = :ownerId ORDER BY plate")
    abstract fun observeByOwner(ownerId: String): Flow<List<VehicleEntity>>
}
