package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction

/**
 * Al cerrar sesión se borran todas las cachés (son datos del taller); la cola
 * `pending_operations` / `pending_uploads` se conserva (docs/06-offline-sync.md §4).
 */
@Dao
abstract class CacheMaintenanceDao {
    @Query("DELETE FROM cache_metadata") protected abstract suspend fun clearMetadata()
    @Query("DELETE FROM branches") protected abstract suspend fun clearBranches()
    @Query("DELETE FROM shifts") protected abstract suspend fun clearShifts()
    @Query("DELETE FROM attendances") protected abstract suspend fun clearAttendances()
    @Query("DELETE FROM tasks") protected abstract suspend fun clearTasks()
    @Query("DELETE FROM work_orders") protected abstract suspend fun clearWorkOrders()
    @Query("DELETE FROM bays") protected abstract suspend fun clearBays()
    @Query("DELETE FROM customers") protected abstract suspend fun clearCustomers()
    @Query("DELETE FROM vehicles") protected abstract suspend fun clearVehicles()
    @Query("DELETE FROM appointments") protected abstract suspend fun clearAppointments()
    @Query("DELETE FROM parts") protected abstract suspend fun clearParts()
    @Query("DELETE FROM batches") protected abstract suspend fun clearBatches()
    @Query("DELETE FROM vouchers") protected abstract suspend fun clearVouchers()
    @Query("DELETE FROM vehicle_faults") protected abstract suspend fun clearVehicleFaults()
    @Query("DELETE FROM predictive_alerts") protected abstract suspend fun clearPredictiveAlerts()

    /** `telemetry_readings` no es caché sino captura pendiente de envío: no se borra aquí. */
    @Transaction
    open suspend fun clearCaches() {
        clearMetadata(); clearBranches(); clearShifts(); clearAttendances(); clearTasks()
        clearWorkOrders(); clearBays(); clearCustomers(); clearVehicles(); clearAppointments()
        clearParts(); clearBatches(); clearVouchers(); clearVehicleFaults(); clearPredictiveAlerts()
    }
}
