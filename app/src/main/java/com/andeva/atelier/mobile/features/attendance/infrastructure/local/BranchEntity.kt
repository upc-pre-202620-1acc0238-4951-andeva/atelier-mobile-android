package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "geofence_radius_m") val geofenceRadiusMeters: Double,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
