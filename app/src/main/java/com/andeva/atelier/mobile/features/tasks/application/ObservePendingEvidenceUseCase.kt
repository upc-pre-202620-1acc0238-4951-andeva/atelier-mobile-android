package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Fotos de la tarea que esperan señal, para mostrarlas con un ícono de "pendiente" junto a las ya registradas.
 * Una `FAILED` se reintenta o descarta desde "Pendientes de sincronizar" (paso 3).
 */
class ObservePendingEvidenceUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(id: TaskId) = taskRepository.observePendingEvidence(id)
}
