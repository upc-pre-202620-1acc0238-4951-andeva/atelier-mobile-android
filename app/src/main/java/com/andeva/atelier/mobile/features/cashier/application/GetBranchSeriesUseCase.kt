package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import javax.inject.Inject

/** Series fiscales de la sede de la sesión. */
class GetBranchSeriesUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(): Result<List<FiscalSeries>> = cashierRepository.getBranchSeries()
}
