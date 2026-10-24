package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Pausar por refrigerio o fin de turno (US27 E2, E3). El backend valida el fin de turno contra el turno
 * vigente con su hora y rechaza la pausa anticipada: mostrar el `message`. En línea.
 */
class PauseTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, reason: PauseReason) = taskRepository.pause(id, reason)
}
