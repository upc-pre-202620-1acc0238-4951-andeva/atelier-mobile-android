package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Elimina un servicio del catálogo del taller si no está asociado a tareas existentes (`operations:services:manage`). En línea. */
class DeleteServiceUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(serviceId: String): Result<Unit> =
        workOrderRepository.deleteService(serviceId)
}
