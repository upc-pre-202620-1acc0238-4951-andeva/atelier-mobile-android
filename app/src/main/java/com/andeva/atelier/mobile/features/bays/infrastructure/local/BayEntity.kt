package com.andeva.atelier.mobile.features.bays.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "bays", indices = [Index("branch_id")])
data class BayEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    val name: String,
    @ColumnInfo(name = "bay_type") val bayType: String,                  // LIFT | PAINT_BOOTH | WASHING | ALIGNMENT
    val status: String,                                                  // AVAILABLE | OCCUPIED | MAINTENANCE
    @ColumnInfo(name = "current_work_order_id") val currentWorkOrderId: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
