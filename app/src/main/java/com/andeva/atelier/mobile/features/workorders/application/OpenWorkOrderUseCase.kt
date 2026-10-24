package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/**
 * Abrir la orden con kilometraje y síntomas en la sede de la sesión (US12 E1). 409 = el vehículo ya tiene una orden
 * abierta (mostrar `message`). Para una cita que llegó no se usa: el backend ya la crea (H10). En línea.
 */
class OpenWorkOrderUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(workOrder: NewWorkOrder): Result<WorkOrder> {
        val invalid = when {
            !WorkOrderValidation.isMileage(workOrder.mileageIn) -> "mileageIn"
            !WorkOrderValidation.isOptionalText(workOrder.diagnosticSummary, WorkOrderValidation.DIAGNOSTIC_MAX) -> "diagnosticSummary"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.open(workOrder)
    }
}
