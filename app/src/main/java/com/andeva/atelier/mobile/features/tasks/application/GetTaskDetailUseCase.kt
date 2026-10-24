package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Detalle en línea: servicio, productos imputados y evidencias (US26, US23, US28). Lo usan también los
 * supervisores al abrir una tarea ajena desde un push (`TASK_PAUSED`, `TASK_SUSPENSION_REQUESTED`, `TASK_PART_CHARGED`).
 */
class GetTaskDetailUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId) = taskRepository.getDetail(id)
}
