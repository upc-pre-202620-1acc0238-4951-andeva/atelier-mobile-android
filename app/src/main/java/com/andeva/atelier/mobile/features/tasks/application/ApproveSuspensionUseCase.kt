package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/** Supervisor: aprobar la suspensión; la tarea pasa a `ON_HOLD` y se congela el cronómetro (US29 E1). En línea. */
class ApproveSuspensionUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId) = taskRepository.approveSuspension(id)
}
