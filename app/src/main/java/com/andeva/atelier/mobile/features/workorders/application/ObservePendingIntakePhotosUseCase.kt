package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Fotos de recepción en cola de la orden, con ícono de "pendiente": `localFilePath` hasta que sube, luego `imageUrl`.
 * Junto con `intakeImages` del detalle forman la galería (US13 E2). `FAILED` se maneja en "Pendientes de sincronizar".
 */
class ObservePendingIntakePhotosUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    operator fun invoke(id: String) = workOrderRepository.observePendingIntakePhotos(id)
}
