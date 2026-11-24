package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.InvalidBayInputException
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import javax.inject.Inject

/** Restaurar una bahía de mantenimiento a disponible (`PUT /api/v1/work-bays/{bayId}/restore`, `operations:bays:manage`). En línea. */
class RestoreBayUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(bayId: String): Result<WorkBay> {
        if (bayId.isBlank()) return Result.failure(InvalidBayInputException("bayId"))
        return bayRepository.restoreBay(bayId)
    }
}
