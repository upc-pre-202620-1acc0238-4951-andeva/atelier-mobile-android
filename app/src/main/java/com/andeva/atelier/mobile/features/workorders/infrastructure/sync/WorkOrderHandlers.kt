package com.andeva.atelier.mobile.features.workorders.infrastructure.sync

import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.AttachImageRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.AttachIntakeImagePayload
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.SubmitProposalPayload
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.SubmitProposalRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.google.gson.Gson
import javax.inject.Inject

/**
 * `ATTACH_INTAKE_IMAGE` (docs/06-offline-sync.md §3, US13). Depende de su `UPLOAD_FILE`, así que la foto ya está en
 * Supabase. Reintentar con la misma `imageUrl` devuelve la imagen ya guardada (R4): no hay códigos de "ya aplicada".
 * Las imágenes no se guardan en Room: no hay caché que refrescar.
 */
class AttachIntakeImageHandler @Inject constructor(
    private val service: WorkOrderService,
    private val gson: Gson
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val payload = gson.fromJson(operation.payload, AttachIntakeImagePayload::class.java)
        val body = AttachImageRequestDto(payload.imageUrl, payload.description)
        return OperationResult.from(apiCall(gson, { service.attachIntakeImage(payload.workOrderId, body) }) { it })
    }

    companion object {
        const val TYPE = "ATTACH_INTAKE_IMAGE"
    }
}

/**
 * `SUBMIT_PROPOSAL` (docs/06-offline-sync.md §3, US14). Igual que [AttachIntakeImageHandler]: la misma `imageUrl`
 * en la orden devuelve la propuesta ya registrada (R4). El backend avisa a supervisión al registrarla.
 */
class SubmitProposalHandler @Inject constructor(
    private val service: WorkOrderService,
    private val gson: Gson
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val payload = gson.fromJson(operation.payload, SubmitProposalPayload::class.java)
        val body = SubmitProposalRequestDto(
            mechanicId = payload.mechanicId,
            description = payload.description,
            severity = payload.severity,
            imageUrl = payload.imageUrl,
            suggestedServiceId = payload.suggestedServiceId
        )
        return OperationResult.from(apiCall(gson, { service.submitProposal(payload.workOrderId, body) }) { it })
    }

    companion object {
        const val TYPE = "SUBMIT_PROPOSAL"
    }
}
