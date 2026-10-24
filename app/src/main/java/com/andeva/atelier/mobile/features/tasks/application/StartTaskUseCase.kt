package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Iniciar la tarea o reanudarla tras una pausa propia (US27 E1, E5; el backend avisa a supervisión con
 * `TASK_RESUMED`). No levanta una suspensión: eso es [ResumeSuspendedTaskUseCase]. En línea.
 */
class StartTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId) = taskRepository.start(id)
}
