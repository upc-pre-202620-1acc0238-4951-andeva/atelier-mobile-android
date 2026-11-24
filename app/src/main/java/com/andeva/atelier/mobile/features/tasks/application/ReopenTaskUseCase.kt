package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Reabre una tarea cerrada para retrabajo de calidad (`POST /api/v1/tasks/{taskId}/reopen`).
 * Requiere `operations:work_orders:update` en la sesión y conexión en línea.
 */
class ReopenTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, reason: String? = null): Result<TaskDetail> =
        taskRepository.reopen(id, reason?.trim()?.takeIf { it.isNotEmpty() })
}
