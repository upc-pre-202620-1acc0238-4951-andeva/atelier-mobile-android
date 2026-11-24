package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.InvalidBayInputException
import javax.inject.Inject

/** Liberar la bahía ocupada por una orden (`DELETE /api/v1/work-orders/{workOrderId}/bay`, `operations:bays:reassign`). En línea. */
class ReleaseBayUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(workOrderId: String): Result<Unit> {
        if (workOrderId.isBlank()) return Result.failure(InvalidBayInputException("workOrderId"))
        return bayRepository.releaseBay(workOrderId)
    }
}
