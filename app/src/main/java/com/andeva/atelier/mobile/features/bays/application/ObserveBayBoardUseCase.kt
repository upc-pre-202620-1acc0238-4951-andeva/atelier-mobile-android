package com.andeva.atelier.mobile.features.bays.application

import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Tablero de bahías con su ocupante, desde Room. */
class ObserveBayBoardUseCase @Inject constructor(
    private val bayRepository: BayRepository
) {
    operator fun invoke(): Flow<List<BayBoardItem>> = bayRepository.observeBoard()
}
