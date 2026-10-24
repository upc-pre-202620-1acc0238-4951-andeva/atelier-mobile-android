package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.PartMovement
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Auditoría de reintegros (US33 E2): cada descargo por lote con lo devuelto, quién y cuándo. Requiere
 * `inventory:batches:read` o `inventory:batches:restore`. En línea.
 */
class GetPartMovementsUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String): Result<List<PartMovement>> = workOrderRepository.getPartMovements(id)
}
