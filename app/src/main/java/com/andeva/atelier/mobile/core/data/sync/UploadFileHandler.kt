package com.andeva.atelier.mobile.core.data.sync

import android.content.Context
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.media.SupabaseUploader
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Payload de `UPLOAD_FILE`: id de la fila en `pending_uploads`. */
data class UploadFilePayload(val uploadId: String)

/**
 * `UPLOAD_FILE`: sube la foto guardada en `filesDir/pending`. La operación `ATTACH_*` que depende de esta
 * ya lleva la URL pública en su payload (es determinista), así que solo espera a que esta termine.
 */
class UploadFileHandler @Inject constructor(
    private val uploadDao: PendingUploadDao,
    private val uploader: SupabaseUploader,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val payload = gson.fromJson(operation.payload, UploadFilePayload::class.java)
        val upload = uploadDao.findById(payload.uploadId) ?: return OperationResult.Applied   // ya se subió antes
        val file = File(upload.localFilePath)
        if (!file.exists()) {
            return OperationResult.Failed(null, "FILE_NOT_FOUND", context.getString(R.string.error_pending_photo_missing))
        }

        val result = uploader.upload(file, upload.storagePath)
        result.onSuccess { url ->
            uploadDao.setPublicUrl(upload.id, url)
            // La foto ya está en Supabase: se libera el almacenamiento local.
            file.delete()
            uploadDao.delete(upload.id)
        }
        return OperationResult.from(result)
    }

    companion object {
        const val TYPE = "UPLOAD_FILE"
    }
}
