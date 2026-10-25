package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import javax.inject.Inject

/** Activa o desactiva una serie fiscal. */
class SetSeriesActiveUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(id: String, active: Boolean): Result<Unit> = cashierRepository.setSeriesActive(id, active)
}
