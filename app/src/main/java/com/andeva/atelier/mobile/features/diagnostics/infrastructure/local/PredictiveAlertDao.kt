package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Caché `predictive_alerts` (docs/06-offline-sync.md §4, por vehículo · al abrir). */
@Dao
abstract class PredictiveAlertDao {
    @Upsert
    abstract suspend fun upsert(alert: PredictiveAlertEntity)

    @Upsert
    abstract suspend fun upsertAll(alerts: List<PredictiveAlertEntity>)

    @Query("DELETE FROM predictive_alerts WHERE vehicle_id = :vehicleId")
    protected abstract suspend fun deleteForVehicle(vehicleId: String)

    @Transaction
    open suspend fun replaceForVehicle(vehicleId: String, alerts: List<PredictiveAlertEntity>) {
        deleteForVehicle(vehicleId)
        upsertAll(alerts)
    }

    @Query("SELECT * FROM predictive_alerts WHERE vehicle_id = :vehicleId ORDER BY created_at DESC")
    abstract fun observeForVehicle(vehicleId: String): Flow<List<PredictiveAlertEntity>>
}
