package com.andeva.atelier.mobile.core.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO en memoria con la misma semántica que las consultas de [PendingOperationDao]. */
class FakePendingOperationDao : PendingOperationDao {
    private val rows = MutableStateFlow<Map<String, PendingOperationEntity>>(linkedMapOf())

    fun all(): Map<String, PendingOperationEntity> = rows.value

    private fun update(id: String, change: (PendingOperationEntity) -> PendingOperationEntity) {
        rows.value[id]?.let { rows.value = rows.value + (id to change(it)) }
    }

    override suspend fun enqueue(operation: PendingOperationEntity): Long {
        if (rows.value.values.any { it.dedupeKey == operation.dedupeKey }) return -1
        rows.value = rows.value + (operation.id to operation)
        return rows.value.size.toLong()
    }

    override suspend fun nextBatch(userId: String, now: Long) = rows.value.values
        .filter { it.userId == userId && it.status == PendingOperationEntity.STATUS_PENDING && it.nextAttemptAt <= now }
        .sortedBy { it.createdAt }

    override suspend fun findById(id: String) = rows.value[id]

    override suspend fun findByDedupeKey(dedupeKey: String) = rows.value.values.firstOrNull { it.dedupeKey == dedupeKey }

    override fun observeByDedupeKey(dedupeKey: String): Flow<PendingOperationEntity?> =
        rows.map { map -> map.values.firstOrNull { it.dedupeKey == dedupeKey } }

    override suspend fun nextAttemptAt(userId: String) = rows.value.values
        .filter { it.userId == userId && it.status == PendingOperationEntity.STATUS_PENDING }
        .minOfOrNull { it.nextAttemptAt }

    override suspend fun markSyncing(id: String) = update(id) { it.copy(status = PendingOperationEntity.STATUS_SYNCING) }

    override suspend fun markRetry(id: String, attempts: Int, nextAttemptAt: Long, httpStatus: Int?, code: String?, message: String?) =
        update(id) {
            it.copy(
                status = PendingOperationEntity.STATUS_PENDING, attempts = attempts, nextAttemptAt = nextAttemptAt,
                lastHttpStatus = httpStatus, lastErrorCode = code, lastErrorMessage = message
            )
        }

    override suspend fun markFailed(id: String, attempts: Int, httpStatus: Int?, code: String?, message: String?) =
        update(id) {
            it.copy(
                status = PendingOperationEntity.STATUS_FAILED, attempts = attempts,
                lastHttpStatus = httpStatus, lastErrorCode = code, lastErrorMessage = message
            )
        }

    override suspend fun dependentIds(parentId: String) = rows.value.values
        .filter { it.dependsOn == parentId && it.status != PendingOperationEntity.STATUS_FAILED }
        .map { it.id }

    override suspend fun failAsDependent(id: String, code: String?, message: String?) =
        update(id) { it.copy(status = PendingOperationEntity.STATUS_FAILED, lastErrorCode = code, lastErrorMessage = message) }

    override suspend fun resetSyncing(userId: String) {
        rows.value.values.filter { it.userId == userId && it.status == PendingOperationEntity.STATUS_SYNCING }
            .forEach { op -> update(op.id) { it.copy(status = PendingOperationEntity.STATUS_PENDING) } }
    }

    override suspend fun retry(id: String, now: Long) =
        update(id) { it.copy(status = PendingOperationEntity.STATUS_PENDING, attempts = 0, nextAttemptAt = now) }

    override suspend fun resetFailed(id: String, now: Long) {
        if (rows.value[id]?.status == PendingOperationEntity.STATUS_FAILED) retry(id, now)
    }

    override suspend fun allDependentIds(parentId: String) = rows.value.values.filter { it.dependsOn == parentId }.map { it.id }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAll(ids: List<String>) {
        rows.value = rows.value - ids.toSet()
    }

    override fun countPending(userId: String): Flow<Int> = rows.map { map -> map.values.count { it.userId == userId } }

    override fun observeForUser(userId: String): Flow<List<PendingOperationEntity>> =
        rows.map { map -> map.values.filter { it.userId == userId }.sortedBy { it.createdAt } }

    override suspend fun deleteForUser(userId: String) {
        rows.value = rows.value.filterValues { it.userId != userId }
    }
}
