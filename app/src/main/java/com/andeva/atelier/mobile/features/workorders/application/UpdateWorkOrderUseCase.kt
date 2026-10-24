package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import javax.inject.Inject

/**
 * Corregir kilometraje o diagnóstico (US12 E1). Es el paso que completa la orden que el backend crea al llegar una
 * cita con kilometraje 0 (H10). Lo que va en null no cambia. En línea.
 */
class UpdateWorkOrderUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String, mileageIn: Int? = null, diagnosticSummary: String? = null): Result<WorkOrder> {
        val invalid = when {
            mileageIn != null && !WorkOrderValidation.isMileage(mileageIn) -> "mileageIn"
            !WorkOrderValidation.isOptionalText(diagnosticSummary, WorkOrderValidation.DIAGNOSTIC_MAX) -> "diagnosticSummary"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.update(id, mileageIn, diagnosticSummary)
    }
}
