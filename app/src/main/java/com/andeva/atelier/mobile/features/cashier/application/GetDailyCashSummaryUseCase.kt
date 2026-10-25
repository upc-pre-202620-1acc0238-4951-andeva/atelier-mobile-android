package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.DailyCashSummary
import java.time.LocalDate
import javax.inject.Inject

/** Caja de la sede de la sesión en un día; sin sede falla con MissingBranchException. */
class GetDailyCashSummaryUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(date: LocalDate): Result<DailyCashSummary> = cashierRepository.getDailyCashSummary(date)
}
