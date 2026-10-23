package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface CacheMetadataDao {
    @Upsert
    suspend fun upsert(metadata: CacheMetadataEntity)

    @Query("SELECT * FROM cache_metadata WHERE scope = :scope")
    suspend fun get(scope: String): CacheMetadataEntity?
}
