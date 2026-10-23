package com.andeva.atelier.mobile.features.inventory.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Se llena solo con inventory:batches:read. */
@Entity(tableName = "batches", indices = [Index("item_id")])
data class BatchEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "batch_number") val batchNumber: String,
    @ColumnInfo(name = "remaining_qty") val remainingQty: String,
    @ColumnInfo(name = "unit_cost") val unitCost: String,
    @ColumnInfo(name = "arrival_date") val arrivalDate: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
