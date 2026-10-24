package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/**
 * Supervisor: retirar un repuesto improcedente; las unidades vuelven a su lote de origen (US23 E2, US33 E1).
 * La auditoría del reintegro (US33 E2) es de `workorders`. En línea.
 */
class RemovePartFromTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, productId: String) = taskRepository.removePart(id, productId)
}
