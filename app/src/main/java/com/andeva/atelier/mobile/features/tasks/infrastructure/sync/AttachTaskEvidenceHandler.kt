package com.andeva.atelier.mobile.features.tasks.infrastructure.sync

import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AttachTaskEvidencePayload
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AttachTaskEvidenceRequestDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskService
import com.google.gson.Gson
import javax.inject.Inject

/**
 * `ATTACH_TASK_EVIDENCE` (docs/06-offline-sync.md §3). Depende de su `UPLOAD_FILE`, así que la foto ya está
 * en Supabase. Reintentar con la misma `imageUrl` devuelve la evidencia ya guardada (R4), por eso no hay
 * códigos de "ya aplicada". Las evidencias no se guardan en Room: no hay caché que refrescar.
 */
class AttachTaskEvidenceHandler @Inject constructor(
    private val service: TaskService,
    private val gson: Gson
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val payload = gson.fromJson(operation.payload, AttachTaskEvidencePayload::class.java)
        val body = AttachTaskEvidenceRequestDto(payload.imageUrl, payload.description)
        return OperationResult.from(apiCall(gson, { service.attachEvidence(payload.taskId, body) }) { it })
    }

    companion object {
        const val TYPE = "ATTACH_TASK_EVIDENCE"
    }
}
