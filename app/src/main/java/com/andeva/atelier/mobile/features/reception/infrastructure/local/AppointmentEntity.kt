package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "appointments", indices = [Index(value = ["branch_id", "scheduled_at"])])
data class AppointmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "scheduled_at") val scheduledAt: Long,
    @ColumnInfo(name = "estimated_duration_minutes") val estimatedDurationMinutes: Int?,
    val reason: String?,
    val status: String,                                                  // PENDING | CONFIRMED | ARRIVED | CANCELED
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
