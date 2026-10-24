package com.andeva.atelier.mobile.features.tasks.infrastructure.repository

import com.andeva.atelier.mobile.core.media.PhotoCapture
import com.andeva.atelier.mobile.core.media.StoragePaths
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AttachTaskEvidencePayload
import com.andeva.atelier.mobile.features.tasks.infrastructure.sync.AttachTaskEvidenceHandler
import java.io.File
import javax.inject.Inject

/** Fotos de desmontaje y montaje (US28): `UPLOAD_FILE` + `ATTACH_TASK_EVIDENCE` con [PhotoCapture]. */
class TaskEvidenceCapture @Inject constructor(
    private val photoCapture: PhotoCapture
) {
    suspend fun capture(taskId: String, workOrderId: String, photo: File, description: String): Result<Unit> =
        photoCapture.capture(
            photo = photo,
            target = TARGET,
            storagePath = { tenantId, photoId -> StoragePaths.taskEvidence(tenantId, workOrderId, taskId, photoId) },
            attachType = AttachTaskEvidenceHandler.TYPE,
            attachDedupeKey = { photoId -> evidenceDedupeKey(taskId, photoId) },
            attachPayload = { imageUrl -> AttachTaskEvidencePayload(taskId, imageUrl, description) }
        ).map { }

    companion object {
        const val TARGET = "TASK_EVIDENCE"

        fun uploadDedupeKey(photoId: String) = PhotoCapture.uploadDedupeKey(photoId)

        /** El prefijo `TASK_EVIDENCE:{taskId}:` agrupa las fotos en cola de una tarea; el sufijo es el id de la foto. */
        fun evidenceDedupeKey(taskId: String, photoId: String) = "${evidencePrefix(taskId)}$photoId"

        fun evidencePrefix(taskId: String) = "TASK_EVIDENCE:$taskId:"
    }
}
