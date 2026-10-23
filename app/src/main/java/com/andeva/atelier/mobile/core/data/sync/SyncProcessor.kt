package com.andeva.atelier.mobile.core.data.sync

import kotlin.math.min

/**
 * Algoritmo del SyncWorker (docs/06-offline-sync.md §3), sin dependencias de Android para poder probarlo.
 */
class SyncProcessor(
    private val dao: PendingOperationDao,
    private val handlers: Map<String, OperationHandler>,
    private val maxAttemptsMessage: String,
    private val now: () -> Long = System::currentTimeMillis
) {
    enum class Outcome { COMPLETED, STOPPED_UNAUTHORIZED }

    suspend fun run(userId: String): Outcome {
        dao.resetSyncing(userId)
        for (operation in dao.nextBatch(userId, now())) {
            val parentId = operation.dependsOn
            if (parentId != null && dao.findById(parentId) != null) continue     // el padre aún no se completó
            val handler = handlers[operation.type] ?: continue                     // tipo sin handler: queda PENDING

            dao.markSyncing(operation.id)
            val result = try {
                handler.send(operation)
            } catch (e: Exception) {
                OperationResult.Retry(message = e.message)
            }

            when (result) {
                OperationResult.Applied -> dao.delete(operation.id)
                OperationResult.Unauthorized -> {
                    dao.markRetry(operation.id, operation.attempts, operation.nextAttemptAt, 401, null, null)
                    return Outcome.STOPPED_UNAUTHORIZED
                }
                is OperationResult.Retry -> {
                    val attempts = operation.attempts + 1
                    if (attempts >= MAX_ATTEMPTS) {
                        fail(operation.id, attempts, result.httpStatus, result.code, maxAttemptsMessage)
                    } else {
                        dao.markRetry(operation.id, attempts, now() + backoffMillis(attempts), result.httpStatus, result.code, result.message)
                    }
                }
                is OperationResult.Failed -> fail(operation.id, operation.attempts + 1, result.httpStatus, result.code, result.message)
            }
        }
        return Outcome.COMPLETED
    }

    /** FAILED en cascada: lo que dependía de esta operación tampoco se podrá enviar. */
    private suspend fun fail(id: String, attempts: Int, httpStatus: Int?, code: String?, message: String?) {
        dao.markFailed(id, attempts, httpStatus, code, message)
        val pending = ArrayDeque(dao.dependentIds(id))
        while (pending.isNotEmpty()) {
            val dependent = pending.removeFirst()
            dao.failAsDependent(dependent, code, message)
            pending.addAll(dao.dependentIds(dependent))
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 8

        /** `min(2^attempts, 60)` minutos. */
        fun backoffMillis(attempts: Int): Long = min(1L shl attempts.coerceAtMost(6), 60L) * 60_000L
    }
}
