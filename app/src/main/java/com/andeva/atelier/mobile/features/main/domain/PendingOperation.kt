package com.andeva.atelier.mobile.features.main.domain

import java.time.Instant

/**
 * Algo capturado sin red que aún no llegó al backend. [type] es el tipo de la cola
 * (`CLOCK_IN`, `UPLOAD_FILE`, `ATTACH_TASK_EVIDENCE`…) y [lastErrorMessage] el `message` del backend.
 */
data class PendingOperation(
    val id: String,
    val type: String,
    val status: PendingOperationStatus,
    val attempts: Int,
    val lastErrorMessage: String?,
    val createdAt: Instant,
    val dependsOn: String?
)
