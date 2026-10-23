package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "vehicle_faults", indices = [Index(value = ["vehicle_id", "dtc_code"])])
data class VehicleFaultEntity(
    @PrimaryKey val id: String,                                          // id del backend, o "local-{uuid}" si aún no se envió
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "dtc_code") val dtcCode: String,
    val severity: String,                                                // LOW | MEDIUM | CRITICAL
    val description: String,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
    @ColumnInfo(name = "is_resolved") val isResolved: Boolean,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?
)
