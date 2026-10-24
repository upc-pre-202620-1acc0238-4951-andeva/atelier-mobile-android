package com.andeva.atelier.mobile.core.media

import android.content.Context
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.data.sync.PendingUploadEntity
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import com.andeva.atelier.mobile.core.data.sync.UploadFilePayload
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import javax.inject.Inject

/**
 * Fotos sin señal (docs/06-offline-sync.md §6, US13, US14, US28): comprimir → guardar en `filesDir/pending` →
 * `pending_uploads` → `UPLOAD_FILE` + la operación `ATTACH_*` de la feature (`depends_on`). Va siempre por la
 * cola, con o sin red: con red el sync se dispara al encolar. La URL pública es determinista, así que el payload
 * de la feature se arma aquí con la URL final.
 */
class PhotoCapture @Inject constructor(
    private val uploader: SupabaseUploader,
    private val uploadDao: PendingUploadDao,
    private val queue: PendingOperationQueue,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    private val clock: Clock
) {
    /**
     * Encola la foto. [storagePath] recibe `tenantId` y el id de la foto ([StoragePaths]); [attachPayload] recibe la
     * URL pública. Devuelve el id de la foto, que es a la vez id de la subida, nombre del archivo y parte de la ruta
     * (06 §7): con él la feature reconoce sus fotos en cola.
     */
    suspend fun capture(
        photo: File,
        target: String,
        storagePath: (tenantId: String, photoId: String) -> String,
        attachType: String,
        attachDedupeKey: (photoId: String) -> String,
        attachPayload: (imageUrl: String) -> Any
    ): Result<String> = runCatching {
        val session = checkNotNull(sessionManager.getSession()) { "No se puede capturar sin sesión" }
        val photoId = StoragePaths.newId()
        val local = uploader.compress(photo, File(context.filesDir, "pending/$photoId.jpg"))
        val path = storagePath(session.tenantId, photoId)
        uploadDao.insert(PendingUploadEntity(photoId, local.path, path, target, createdAt = clock.millis()))

        val uploadId = queue.enqueue(UploadFileHandler.TYPE, UploadFilePayload(photoId), uploadDedupeKey(photoId))
        queue.enqueue(
            type = attachType,
            payload = attachPayload(uploader.publicUrlFor(path)),
            dedupeKey = attachDedupeKey(photoId),
            dependsOn = uploadId
        )
        photoId
    }

    companion object {
        fun uploadDedupeKey(photoId: String) = "UPLOAD_FILE:$photoId"
    }
}
