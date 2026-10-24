package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import javax.inject.Inject

/**
 * Afiliar al taller una cuenta de la app de clientes (US44 E1); sus vehículos quedan vinculados sin volver a
 * digitarlos. El documento es obligatorio (docs/03-traceability.md). En línea.
 */
class AffiliateCustomerUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(email: String, taxId: String, phone: String? = null): Result<Customer> {
        val invalid = when {
            !ReceptionValidation.isEmail(email) -> "email"
            !ReceptionValidation.isPersonTaxId(taxId) -> "taxId"
            !ReceptionValidation.isOptionalPhone(phone) -> "phone"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidReceptionInputException(invalid))
        return receptionRepository.affiliate(email, taxId, phone)
    }
}
