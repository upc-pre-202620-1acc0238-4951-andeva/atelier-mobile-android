package com.andeva.atelier.mobile.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disparadores del SyncWorker: al encolar, al recuperar red, al volver a primer plano
 * y uno periódico cada 15 min (docs/06-offline-sync.md §3).
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(connected).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Reintento tras el backoff. Usa otro nombre para no bloquear un envío inmediato con KEEP. */
    fun requestSyncAt(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        val delay = (epochMillis - nowMillis).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(connected)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(RETRY_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(connected).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val WORK_NAME = "atelier-sync"
        const val RETRY_WORK_NAME = "atelier-sync-retry"
        const val PERIODIC_WORK_NAME = "atelier-sync-periodic"
    }
}
