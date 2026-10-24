package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Al abrir Mis tareas y al tocar el push `TASK_ASSIGNED` (US26 E1/E2). Si falla (sin red), seguir con la
 * caché y mostrar "Actualizado hace N min".
 */
class RefreshMyTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke() = taskRepository.refreshMyTasks()
}
