package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val scope: String,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long,
    @ColumnInfo(name = "record_count") val recordCount: Int
)
