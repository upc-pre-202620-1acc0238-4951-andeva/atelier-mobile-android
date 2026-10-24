package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskValidation
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Cierre técnico con horas hombre en decimal (US30 E1). Si es la última tarea, el backend pasa la orden a
 * `COMPLETED` y avisa con `WORK_ORDER_COMPLETED` (US30 E2). La tarea sale de Mis tareas. En línea.
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, actualLaborHours: BigDecimal, notes: String? = null): Result<TaskDetail> {
        if (!TaskValidation.isLaborHours(actualLaborHours)) return Result.failure(InvalidTaskInputException("actualLaborHours"))
        if (!TaskValidation.isNotes(notes)) return Result.failure(InvalidTaskInputException("notes"))
        return taskRepository.complete(id, actualLaborHours, notes)
    }
}
