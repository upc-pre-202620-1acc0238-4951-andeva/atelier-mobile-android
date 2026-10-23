package com.andeva.atelier.mobile.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.TokenRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Vacía la cola `pending_operations` del usuario actual cuando hay red (docs/06-offline-sync.md §3). */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingOperationDao,
    private val sessionManager: SessionManager,
    private val tokenRefresher: TokenRefresher,
    private val handlers: Map<String, @JvmSuppressWildcards OperationHandler>,
    private val syncScheduler: SyncScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val session = sessionManager.getSession() ?: return Result.success()
        // Sin sesión renovable: las operaciones quedan PENDING para cuando el usuario vuelva a entrar.
        withContext(Dispatchers.IO) { tokenRefresher.ensureFresh() } ?: return Result.success()

        val processor = SyncProcessor(dao, handlers, applicationContext.getString(R.string.sync_failed_max_attempts))
        val outcome = processor.run(session.userId)

        if (outcome == SyncProcessor.Outcome.COMPLETED) {
            dao.nextAttemptAt(session.userId)?.let { next ->
                syncScheduler.requestSyncAt(next)
            }
        }
        return Result.success()
    }
}
