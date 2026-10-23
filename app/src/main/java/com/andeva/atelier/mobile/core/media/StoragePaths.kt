package com.andeva.atelier.mobile.core.media

import java.util.UUID

/**
 * Rutas en el bucket (docs/04-mobile-integration.md §3). El UUID se genera al capturar la foto:
 * dos fotos nunca comparten URL y el reintento de la misma siempre la repite (docs/06-offline-sync.md §7).
 */
object StoragePaths {
    fun intake(tenantId: String, workOrderId: String, photoId: String = newId()) =
        "$tenantId/work-orders/$workOrderId/intake/$photoId.jpg"

    fun taskEvidence(tenantId: String, workOrderId: String, taskId: String, photoId: String = newId()) =
        "$tenantId/work-orders/$workOrderId/tasks/$taskId/$photoId.jpg"

    fun proposal(tenantId: String, workOrderId: String, photoId: String = newId()) =
        "$tenantId/work-orders/$workOrderId/proposals/$photoId.jpg"

    fun newId(): String = UUID.randomUUID().toString()
}
