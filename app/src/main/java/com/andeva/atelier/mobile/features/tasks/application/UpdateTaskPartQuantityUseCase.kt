package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskValidation
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Supervisor: corregir la cantidad de un repuesto imputado ([productId] del catálogo); el backend recalcula la orden
 * (US22 E2). Devuelve la tarea recargada: la respuesta del `PUT` no es confiable (H9). En línea.
 */
class UpdateTaskPartQuantityUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskDetail> {
        if (!TaskValidation.isQuantity(quantity)) return Result.failure(InvalidTaskInputException("quantity"))
        return taskRepository.updatePartQuantity(id, productId, quantity)
    }
}
