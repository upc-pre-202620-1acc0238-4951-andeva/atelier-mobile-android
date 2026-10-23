package com.andeva.atelier.mobile.features.inventory.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "parts", indices = [Index("sku"), Index("category")])
data class PartEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val category: String,
    @ColumnInfo(name = "base_price") val basePrice: String,
    @ColumnInfo(name = "total_stock") val totalStock: String,
    @ColumnInfo(name = "minimum_stock") val minimumStock: String,
    @ColumnInfo(name = "unit_of_measure") val unitOfMeasure: String,
    val status: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
