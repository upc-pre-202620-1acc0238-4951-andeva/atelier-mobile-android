package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Caché `vehicle_faults` (docs/06-offline-sync.md §4, por vehículo en diagnóstico). Las fallas aún en cola tienen id
 * `local-{uuid}` y sobreviven a la recarga desde el backend.
 */
@Dao
abstract class VehicleFaultDao {
    @Upsert
    abstract suspend fun upsert(fault: VehicleFaultEntity)

    @Upsert
    abstract suspend fun upsertAll(faults: List<VehicleFaultEntity>)

    @Query("SELECT * FROM vehicle_faults WHERE id = :id")
    abstract suspend fun findById(id: String): VehicleFaultEntity?

    @Query("DELETE FROM vehicle_faults WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("DELETE FROM vehicle_faults WHERE vehicle_id = :vehicleId AND id NOT LIKE 'local-%'")
    protected abstract suspend fun deleteRemoteForVehicle(vehicleId: String)

    /** Recarga del alcance `faults:{vehicleId}` sin tocar las fallas que siguen en cola. */
    @Transaction
    open suspend fun replaceRemote(vehicleId: String, faults: List<VehicleFaultEntity>) {
        deleteRemoteForVehicle(vehicleId)
        upsertAll(faults)
    }

    /** `REGISTER_FAULT` confirmado: la fila local pasa a tener el id del backend. */
    @Transaction
    open suspend fun replaceLocal(localId: String, fault: VehicleFaultEntity) {
        deleteById(localId)
        upsert(fault)
    }

    @Query("SELECT * FROM vehicle_faults WHERE vehicle_id = :vehicleId AND is_resolved = 0 ORDER BY detected_at DESC")
    abstract suspend fun findActive(vehicleId: String): List<VehicleFaultEntity>

    @Query("SELECT * FROM vehicle_faults WHERE vehicle_id = :vehicleId AND is_resolved = 0 ORDER BY detected_at DESC")
    abstract fun observeActive(vehicleId: String): Flow<List<VehicleFaultEntity>>

    @Query("UPDATE vehicle_faults SET is_resolved = 1, resolved_at = :resolvedAt WHERE id = :id")
    abstract suspend fun markResolved(id: String, resolvedAt: Long)

    @Query("SELECT COUNT(*) FROM vehicle_faults WHERE vehicle_id = :vehicleId AND id LIKE 'local-%'")
    abstract fun observePendingCount(vehicleId: String): Flow<Int>
}
