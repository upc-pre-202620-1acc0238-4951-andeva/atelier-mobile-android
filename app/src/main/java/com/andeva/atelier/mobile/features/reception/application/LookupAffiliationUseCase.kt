package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import javax.inject.Inject

/**
 * Buscar el correo del conductor (US44): `nextStep` dice si ya es cliente, si se afilia su cuenta
 * ([AffiliateCustomerUseCase], E1) o si se registra presencialmente (alta de cliente y vehículo, E2). En línea.
 */
class LookupAffiliationUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(email: String): Result<AffiliationLookup> {
        if (!ReceptionValidation.isEmail(email)) return Result.failure(InvalidReceptionInputException("email"))
        return receptionRepository.lookupAffiliation(email)
    }
}
