package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "vehicles", indices = [Index(value = ["plate"], unique = true)])
data class VehicleEntity(
    @PrimaryKey val id: String,
    val plate: String,
    val vin: String?,
    val brand: String,
    val model: String,
    val year: Int?,
    @ColumnInfo(name = "engine_type") val engineType: String?,
    @ColumnInfo(name = "current_owner_id") val currentOwnerId: String?,
    @ColumnInfo(name = "current_mileage") val currentMileage: Int?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
