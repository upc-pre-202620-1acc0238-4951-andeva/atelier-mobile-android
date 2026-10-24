package com.andeva.atelier.mobile.features.main.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import com.andeva.atelier.mobile.core.data.sync.UploadFilePayload
import com.andeva.atelier.mobile.features.main.domain.PendingOperation
import com.andeva.atelier.mobile.features.main.domain.PendingOperationRepository
import com.andeva.atelier.mobile.features.main.domain.PendingOperationStatus
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import javax.inject.Inject

/**
 * Invariante: SyncProcessor toma "padre ausente" como "padre aplicado", así que nunca se deja en la
 * cola un hijo cuyo padre se descartó.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingOperationRepositoryImpl @Inject constructor(
    private val dao: PendingOperationDao,
    private val uploadDao: PendingUploadDao,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
    private val gson: Gson
) : PendingOperationRepository {

    /** Cambia sola al cambiar de usuario: nunca se ve la cola de otro (06 §3, "Cambio de usuario"). */
    private val currentUserId: Flow<String?> = sessionManager.session.map { it?.userId }.distinctUntilChanged()

    override fun observeAll(): Flow<List<PendingOperation>> = currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else dao.observeForUser(userId).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeCount(): Flow<Int> = currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(0) else dao.countPending(userId)
    }

    override suspend fun retry(id: String): Boolean {
        val operation = ownOperation(id) ?: return false
        if (operation.status != PendingOperationEntity.STATUS_FAILED) return false
        val now = System.currentTimeMillis()

        // Hacia arriba: un padre FAILED (p. ej. la subida de la foto) tiene que salir primero
        var parent = operation.dependsOn?.let { dao.findById(it) }
        while (parent != null) {
            dao.resetFailed(parent.id, now)
            parent = parent.dependsOn?.let { dao.findById(it) }
        }
        dao.resetFailed(operation.id, now)
        // Hacia abajo: los que fallaron en cascada por esta
        descendantsOf(operation.id).forEach { dao.resetFailed(it, now) }

        // Si el worker ya corre, KEEP ignora esto, pero al terminar reprograma con next_attempt_at = now
        syncScheduler.requestSync()
        return true
    }

    override suspend fun discard(id: String): Boolean {
        val operation = ownOperation(id) ?: return false

        // Hacia arriba solo con fotos: una subida sin su ATTACH_* dejaría un archivo huérfano en Supabase.
        // Un CLOCK_IN no se descarta con su CLOCK_OUT: son hechos distintos.
        var root = operation
        while (true) {
            val parent = root.dependsOn?.let { dao.findById(it) } ?: break
            if (parent.type != UploadFileHandler.TYPE) break
            if (dao.allDependentIds(parent.id).any { it != root.id }) break
            root = parent
        }

        val ids = listOf(root.id) + descendantsOf(root.id)
        val operations = ids.mapNotNull { dao.findById(it) }
        // Algo de la cadena se está enviando en este momento: no se toca
        if (operations.any { it.status == PendingOperationEntity.STATUS_SYNCING }) return false

        val uploads = operations
            .filter { it.type == UploadFileHandler.TYPE }
            .mapNotNull { runCatching { gson.fromJson(it.payload, UploadFilePayload::class.java) }.getOrNull() }
            .mapNotNull { uploadDao.findById(it.uploadId) }

        // Primero la cola (si el worker ya tenía la operación en su lote, actualizar una fila ausente no hace nada),
        // después las subidas y al final los archivos
        dao.deleteAll(ids)
        uploads.forEach { uploadDao.delete(it.id) }
        uploads.forEach { File(it.localFilePath).delete() }
        return true
    }

    private suspend fun ownOperation(id: String): PendingOperationEntity? {
        val userId = sessionManager.getSession()?.userId ?: return null
        return dao.findById(id)?.takeIf { it.userId == userId }
    }

    private suspend fun descendantsOf(id: String): List<String> {
        val result = mutableListOf<String>()
        val pending = ArrayDeque(dao.allDependentIds(id))
        while (pending.isNotEmpty()) {
            val child = pending.removeFirst()
            if (child in result) continue
            result += child
            pending.addAll(dao.allDependentIds(child))
        }
        return result
    }

    private fun PendingOperationEntity.toDomain() = PendingOperation(
        id = id,
        type = type,
        status = when (status) {
            PendingOperationEntity.STATUS_SYNCING -> PendingOperationStatus.SYNCING
            PendingOperationEntity.STATUS_FAILED -> PendingOperationStatus.FAILED
            else -> PendingOperationStatus.PENDING
        },
        attempts = attempts,
        lastErrorMessage = lastErrorMessage,
        createdAt = Instant.ofEpochMilli(createdAt),
        dependsOn = dependsOn
    )
}
