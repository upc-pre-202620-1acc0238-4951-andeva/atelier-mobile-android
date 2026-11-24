package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.TenantFiscalProfile
import javax.inject.Inject

/** Obtiene la información fiscal del taller (`GET /api/v1/tenants/current`, US42). */
class GetTenantFiscalProfileUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(): Result<TenantFiscalProfile> =
        cashierRepository.getTenantFiscalProfile()
}
