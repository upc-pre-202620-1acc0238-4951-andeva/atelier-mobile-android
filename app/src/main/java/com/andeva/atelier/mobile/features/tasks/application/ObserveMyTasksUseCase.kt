package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/** Mis tareas abiertas desde Room (en curso → en pausa → asignadas), con placa y bahía si están en caché (US26 E1). */
class ObserveMyTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke() = taskRepository.observeMyTasks()
}
