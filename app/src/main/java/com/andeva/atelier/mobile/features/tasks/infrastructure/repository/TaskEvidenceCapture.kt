package com.andeva.atelier.mobile.features.tasks.infrastructure.repository

import android.content.Context
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.data.sync.PendingUploadEntity
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import com.andeva.atelier.mobile.core.data.sync.UploadFilePayload
import com.andeva.atelier.mobile.core.media.StoragePaths
import com.andeva.atelier.mobile.core.media.SupabaseUploader
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AttachTaskEvidencePayload
import com.andeva.atelier.mobile.features.tasks.infrastructure.sync.AttachTaskEvidenceHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import javax.inject.Inject

/**
 * Fotos de desmontaje y montaje (US28, docs/06-offline-sync.md §6 "Fotos sin señal"): comprimir → guardar en
 * `filesDir/pending` → `pending_uploads` → `UPLOAD_FILE` + `ATTACH_TASK_EVIDENCE` (`depends_on`). Va siempre por
 * la cola, con o sin red: con red el sync se dispara al encolar.
 */
class TaskEvidenceCapture @Inject constructor(
    private val uploader: SupabaseUploader,
    private val uploadDao: PendingUploadDao,
    private val queue: PendingOperationQueue,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    private val clock: Clock
) {
    suspend fun capture(taskId: String, workOrderId: String, photo: File, description: String): Result<Unit> = runCatching {
        val session = checkNotNull(sessionManager.getSession()) { "No se puede capturar sin sesión" }
        // El UUID de la foto es a la vez id de la subida, nombre del archivo y parte de la ruta (06 §7)
        val photoId = StoragePaths.newId()
        val local = uploader.compress(photo, File(context.filesDir, "pending/$photoId.jpg"))
        val storagePath = StoragePaths.taskEvidence(session.tenantId, workOrderId, taskId, photoId)
        uploadDao.insert(PendingUploadEntity(photoId, local.path, storagePath, TARGET, createdAt = clock.millis()))

        val uploadId = queue.enqueue(UploadFileHandler.TYPE, UploadFilePayload(photoId), uploadDedupeKey(photoId))
        queue.enqueue(
            type = AttachTaskEvidenceHandler.TYPE,
            payload = AttachTaskEvidencePayload(taskId, uploader.publicUrlFor(storagePath), description),
            dedupeKey = evidenceDedupeKey(taskId, photoId),
            dependsOn = uploadId
        )
        Unit
    }

    companion object {
        const val TARGET = "TASK_EVIDENCE"

        fun uploadDedupeKey(photoId: String) = "UPLOAD_FILE:$photoId"

        /** El prefijo `TASK_EVIDENCE:{taskId}:` agrupa las fotos en cola de una tarea; el sufijo es el id de la foto. */
        fun evidenceDedupeKey(taskId: String, photoId: String) = "${evidencePrefix(taskId)}$photoId"

        fun evidencePrefix(taskId: String) = "TASK_EVIDENCE:$taskId:"
    }
}
