package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Volver a buscar la orden en borrador que el backend crea tras la llegada de una cita (H10), cuando
 * `MarkAppointmentArrivedUseCase` devolvió `WorkOrderPending`. null = aún no existe. En línea.
 */
class FindDraftWorkOrderForVehicleUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(branchId: String, vehicleId: String): Result<WorkOrder?> =
        workOrderRepository.findDraftForVehicle(branchId, vehicleId)
}
