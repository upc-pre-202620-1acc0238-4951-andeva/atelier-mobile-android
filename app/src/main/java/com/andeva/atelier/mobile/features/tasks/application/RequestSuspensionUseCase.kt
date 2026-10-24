package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskValidation
import javax.inject.Inject

/**
 * Pedir la suspensión por una pieza defectuosa o faltante (US29 E1). Queda pendiente hasta que un supervisor
 * la apruebe (push `TASK_SUSPENSION_REQUESTED`). [inventoryItemId] es el repuesto a reponer, si está en catálogo.
 */
class RequestSuspensionUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, missingItemDescription: String, inventoryItemId: String? = null): Result<TaskDetail> {
        if (!TaskValidation.isMissingItem(missingItemDescription)) {
            return Result.failure(InvalidTaskInputException("missingItemDescription"))
        }
        return taskRepository.requestSuspension(id, missingItemDescription, inventoryItemId)
    }
}
