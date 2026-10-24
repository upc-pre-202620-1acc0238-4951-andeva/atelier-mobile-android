package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Supervisor: levantar la suspensión cuando llega el repuesto conforme (US29 E3; el backend avisa al mecánico
 * con `TASK_RESUMED`). El mecánico no puede hacerlo con `/start` (hueco H6). En línea.
 */
class ResumeSuspendedTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId) = taskRepository.resume(id)
}
