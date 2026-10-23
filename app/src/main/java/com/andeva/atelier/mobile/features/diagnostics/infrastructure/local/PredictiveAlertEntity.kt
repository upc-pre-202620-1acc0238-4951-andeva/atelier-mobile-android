package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "predictive_alerts", indices = [Index(value = ["vehicle_id", "created_at"])])
data class PredictiveAlertEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "alert_type") val alertType: String,              // ENGINE_OVERHEATING_RISK | BATTERY_FAILURE_RISK | …
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double,
    val message: String,
    val status: String,                                                  // DISPATCHED | ACKNOWLEDGED | RESOLVED | DISMISSED
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
