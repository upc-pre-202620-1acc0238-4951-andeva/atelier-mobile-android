package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "start_time") val startTime: String,              // "08:00:00" hora de Lima
    @ColumnInfo(name = "end_time") val endTime: String,
    @ColumnInfo(name = "grace_period_minutes") val gracePeriodMinutes: Int,
    @ColumnInfo(name = "work_days") val workDays: String,                // "MONDAY,TUESDAY,…"
    @ColumnInfo(name = "rest_days") val restDays: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
