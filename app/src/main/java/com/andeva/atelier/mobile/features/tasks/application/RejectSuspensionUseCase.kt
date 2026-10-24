package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/** Supervisor: desestimar la suspensión; la tarea sigue en progreso (US29 E2). En línea. */
class RejectSuspensionUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId) = taskRepository.rejectSuspension(id)
}
