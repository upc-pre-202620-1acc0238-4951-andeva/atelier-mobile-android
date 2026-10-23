package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "attendances", indices = [Index(value = ["membership_id", "clock_in"])])
data class AttendanceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "membership_id") val membershipId: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    @ColumnInfo(name = "clock_in") val clockIn: Long,
    @ColumnInfo(name = "clock_out") val clockOut: Long?,
    val status: String,                                                  // ON_TIME | LATE | EXCUSED | ABSENT
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "distance_to_branch_m") val distanceToBranchMeters: Double,
    @ColumnInfo(name = "worked_minutes") val workedMinutes: Long?,
    @ColumnInfo(name = "regular_minutes") val regularMinutes: Long?,
    @ColumnInfo(name = "overtime_minutes") val overtimeMinutes: Long?,
    @ColumnInfo(name = "justification_reason") val justificationReason: String?,
    @ColumnInfo(name = "justification_status") val justificationStatus: String?,   // PENDING | APPROVED | REJECTED
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
