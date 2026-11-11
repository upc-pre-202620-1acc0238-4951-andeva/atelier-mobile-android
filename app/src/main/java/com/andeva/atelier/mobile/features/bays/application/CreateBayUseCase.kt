package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import javax.inject.Inject

/** US15 / US16: registrar una nueva bahía física en el taller (`POST /api/v1/work-bays`). */
class CreateBayUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(name: String, bayType: BayType): Result<WorkBay> =
        bayRepository.createBay(name, bayType)
}
