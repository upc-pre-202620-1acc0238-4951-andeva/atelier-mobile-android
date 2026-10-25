package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import javax.inject.Inject

/** Actualiza el tablero de bahías de la sede de la sesión. Sin sede falla con `MissingBranchException`. */
class RefreshBayBoardUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    suspend operator fun invoke(): Result<Unit> = bayRepository.refreshBranchBays()
}
