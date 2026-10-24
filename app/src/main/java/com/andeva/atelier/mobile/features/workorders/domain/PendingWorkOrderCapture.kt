package com.andeva.atelier.mobile.features.workorders.domain

import java.time.Instant

/**
 * Foto de recepción en cola (US13, docs/06-offline-sync.md §6). [localFilePath] existe hasta que se sube; luego
 * vale [imageUrl]. `FAILED` se maneja en "Pendientes de sincronizar" (paso 3).
 */
data class PendingIntakePhoto(
    val operationId: String,
    val localFilePath: String?,
    val imageUrl: String,
    val description: String?,
    val status: String,
    val lastErrorMessage: String?,
    val capturedAt: Instant
)

/** Propuesta capturada que espera enviarse con su foto (US14). Igual que [PendingIntakePhoto]. */
data class PendingProposal(
    val operationId: String,
    val localFilePath: String?,
    val imageUrl: String,
    val description: String,
    val severity: ProposalSeverity?,
    val status: String,
    val lastErrorMessage: String?,
    val capturedAt: Instant
)
