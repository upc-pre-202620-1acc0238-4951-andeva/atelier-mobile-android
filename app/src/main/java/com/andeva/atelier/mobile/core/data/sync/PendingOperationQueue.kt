package com.andeva.atelier.mobile.core.data.sync

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Punto único para encolar lo que se captura sin red. Los casos de uso de cada feature lo usan. */
@Singleton
class PendingOperationQueue @Inject constructor(
    private val dao: PendingOperationDao,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
    private val gson: Gson
) {
    /**
     * Encola [payload] (el body exacto a enviar, ya con la hora del dispositivo) y dispara el sync.
     * Devuelve el id de la operación, o null si el mismo hecho (`dedupeKey`) ya estaba en cola.
     */
    suspend fun enqueue(
        type: String,
        payload: Any,
        dedupeKey: String,
        dependsOn: String? = null,
        id: String = UUID.randomUUID().toString(),
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        val session = checkNotNull(sessionManager.getSession()) { "No se puede encolar sin sesión" }
        val inserted = dao.enqueue(
            PendingOperationEntity(
                id = id,
                type = type,
                payload = gson.toJson(payload),
                dedupeKey = dedupeKey,
                userId = session.userId,
                tenantId = session.tenantId,
                nextAttemptAt = nowMillis,
                createdAt = nowMillis,
                dependsOn = dependsOn
            )
        )
        if (inserted == -1L) return null
        syncScheduler.requestSync()
        return id
    }
}
