package com.andeva.atelier.mobile.core.data.sync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Cola única de lo capturado sin red (docs/06-offline-sync.md §3). Las exitosas se borran. */
@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["dedupe_key"], unique = true), Index(value = ["status", "next_attempt_at"]), Index("user_id")]
)
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payload: String,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "tenant_id") val tenantId: String,
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    @ColumnInfo(name = "next_attempt_at") val nextAttemptAt: Long,
    @ColumnInfo(name = "last_http_status") val lastHttpStatus: Int? = null,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String? = null,
    @ColumnInfo(name = "last_error_message") val lastErrorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "depends_on") val dependsOn: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCING = "SYNCING"
        const val STATUS_FAILED = "FAILED"
    }
}
