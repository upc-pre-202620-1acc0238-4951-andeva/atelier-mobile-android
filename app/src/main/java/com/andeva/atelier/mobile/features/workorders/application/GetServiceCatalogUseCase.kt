package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Servicios del taller para elegir al agregar una tarea o aprobar una propuesta (US12 E2, US15 E1) y para dar nombre
 * a las tareas del detalle por `serviceId` (H11). Sin `operations:services:read`, lista vacía. En línea.
 */
class GetServiceCatalogUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(): Result<List<ServiceOption>> = workOrderRepository.getServiceCatalog()
}
