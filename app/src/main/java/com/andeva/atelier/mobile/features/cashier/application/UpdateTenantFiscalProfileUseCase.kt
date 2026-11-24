package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.TenantFiscalProfile
import javax.inject.Inject

/** Actualiza la información fiscal del taller (`PUT /api/v1/tenants/current`, US42). */
class UpdateTenantFiscalProfileUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(
        name: String,
        legalName: String
    ): Result<TenantFiscalProfile> =
        cashierRepository.updateTenantFiscalProfile(name, legalName)
}
