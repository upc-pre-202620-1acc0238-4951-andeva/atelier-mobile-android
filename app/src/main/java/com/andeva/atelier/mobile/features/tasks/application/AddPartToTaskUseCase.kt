package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskValidation
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Imputar un repuesto con el precio de catálogo (US23 E1/E3, US35 E1). El mecánico solo a su tarea abierta
 * (403 si no); stock insuficiente → 409 con el `message` (US32 E3). En línea.
 */
class AddPartToTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskProduct> {
        if (!TaskValidation.isQuantity(quantity)) return Result.failure(InvalidTaskInputException("quantity"))
        return taskRepository.addPart(id, productId, quantity)
    }
}
