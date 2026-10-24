package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Upsert
    suspend fun upsert(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    fun observeById(id: String): Flow<ShiftEntity?>
}
