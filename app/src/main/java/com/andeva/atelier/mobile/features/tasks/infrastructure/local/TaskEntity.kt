package com.andeva.atelier.mobile.features.tasks.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "tasks", indices = [Index("work_order_id")])
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "work_order_id") val workOrderId: String,
    @ColumnInfo(name = "work_order_number") val workOrderNumber: Int?,
    @ColumnInfo(name = "work_order_status") val workOrderStatus: String?,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "branch_id") val branchId: String?,
    @ColumnInfo(name = "bay_id") val bayId: String?,
    @ColumnInfo(name = "service_name") val serviceName: String?,          // del detalle
    @ColumnInfo(name = "mechanic_id") val mechanicId: String?,
    val status: String,                                                  // ASSIGNED | IN_PROGRESS | ON_HOLD | COMPLETED | …
    val description: String,
    @ColumnInfo(name = "estimated_hours") val estimatedHours: String?,
    @ColumnInfo(name = "hold_reason") val holdReason: String?,           // BREAK | END_OF_SHIFT | DEFECTIVE_PART | …
    @ColumnInfo(name = "missing_item_description") val missingItemDescription: String?,
    @ColumnInfo(name = "pending_suspension_reason") val pendingSuspensionReason: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long?,
    @ColumnInfo(name = "total_paused_seconds") val totalPausedSeconds: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
