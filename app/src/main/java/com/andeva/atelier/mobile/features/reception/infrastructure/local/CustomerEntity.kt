package com.andeva.atelier.mobile.features.reception.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "customers", indices = [Index("tax_id")])
data class CustomerEntity(
    @PrimaryKey val id: String,
    val type: String,                                                    // INDIVIDUAL | COMPANY
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "tax_id") val taxId: String?,
    val email: String?,
    val phone: String?,
    val status: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
