package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Colaboradores activos de la sede para asignar una tarea (US12 E2). Sin `iam:members:read` (el Asesor no lo tiene)
 * devuelve lista vacía y la tarea se crea sin mecánico (H11). En línea.
 */
class GetBranchMechanicsUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(): Result<List<MechanicOption>> = workOrderRepository.getBranchMechanics()
}
