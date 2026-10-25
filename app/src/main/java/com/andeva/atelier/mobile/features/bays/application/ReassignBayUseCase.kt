package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.BayUnavailableException
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import javax.inject.Inject

/**
 * Mueve la orden a otra bahía. Si [WorkBay.canReceive] es falso devuelve [BayUnavailableException] sin llamar;
 * el backend igual vuelve a validar (409).
 */
class ReassignBayUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(workOrderId: String, targetBay: WorkBay): Result<Unit> {
        if (!targetBay.canReceive) return Result.failure(BayUnavailableException())
        return bayRepository.reassignBay(workOrderId, targetBay.id)
    }
}
