package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import javax.inject.Inject

/** Cambia el mecánico de una tarea. Los candidatos salen de `GetBranchMechanicsUseCase` (paso 6). */
class ReassignTaskMechanicUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(taskId: String, mechanicId: String): Result<Unit> =
        bayRepository.reassignMechanic(taskId, mechanicId)
}
