package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/** Una tarea propia desde Room, para mostrarla sin red. null si no es propia o ya se cerró. */
class ObserveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(id: TaskId) = taskRepository.observeTask(id)
}
