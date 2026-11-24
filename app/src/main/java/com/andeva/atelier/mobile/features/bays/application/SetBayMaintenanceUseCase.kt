package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.InvalidBayInputException
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import javax.inject.Inject

/** Poner una bahía en mantenimiento (`PUT /api/v1/work-bays/{bayId}/maintenance`, `operations:bays:manage`). En línea. */
class SetBayMaintenanceUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(bayId: String, reason: String): Result<WorkBay> {
        if (bayId.isBlank()) return Result.failure(InvalidBayInputException("bayId"))
        val cleanReason = reason.trim()
        if (cleanReason.isEmpty() || cleanReason.length > REASON_MAX_LENGTH) {
            return Result.failure(InvalidBayInputException("reason"))
        }
        return bayRepository.setBayMaintenance(bayId, cleanReason)
    }

    companion object {
        const val REASON_MAX_LENGTH = 1000
    }
}
