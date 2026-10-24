package com.andeva.atelier.mobile.features.tasks.domain

import java.time.Instant

/**
 * Foto de la tarea capturada y aún en la cola. [localFilePath] existe hasta que la foto sube a Supabase;
 * después se muestra [imageUrl] hasta que el backend la registre. [status] es el de `pending_operations`
 * (`PENDING`, `SYNCING`, `FAILED`) y [lastErrorMessage] el `message` del backend.
 */
data class PendingEvidence(
    val operationId: String,
    val localFilePath: String?,
    val imageUrl: String,
    val description: String?,
    val status: String,
    val lastErrorMessage: String?,
    val capturedAt: Instant
)
