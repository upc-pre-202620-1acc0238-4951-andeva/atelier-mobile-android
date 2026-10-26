package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import javax.inject.Inject

/** Registra una serie fiscal en la sede de la sesión. */
class ConfigureSeriesUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(type: VoucherType, serie: String, initialCorrelative: Int? = null): Result<FiscalSeries> =
        cashierRepository.configureSeries(type, serie, initialCorrelative)
}
