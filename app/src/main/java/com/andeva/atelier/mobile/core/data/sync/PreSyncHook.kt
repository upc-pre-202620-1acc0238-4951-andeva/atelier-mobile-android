package com.andeva.atelier.mobile.core.data.sync

/**
 * Gancho ejecutado dentro de `SyncProcessor.runLock` antes de drenar `pending_operations`, y al inicio de
 * `signOut()` con [forceFlushAll] = true para empaquetar lecturas locales pendientes del usuario saliente
 * (p. ej. telemetría OBD-II `< 200` filas en fosa tras muerte de proceso, US19).
 */
interface PreSyncHook {
    suspend fun beforeSync(userId: String, forceFlushAll: Boolean = false)
}
