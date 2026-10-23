package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pending_uploads")
data class PendingUploadEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "local_file_path") val localFilePath: String,     // filesDir/pending/{id}.jpg
    @ColumnInfo(name = "storage_path") val storagePath: String,          // {tenantId}/work-orders/{woId}/tasks/{taskId}/{id}.jpg
    val target: String,                                                  // INTAKE | TASK_EVIDENCE | PROPOSAL
    @ColumnInfo(name = "public_url") val publicUrl: String? = null,      // se llena al subir
    @ColumnInfo(name = "created_at") val createdAt: Long
)
