package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.NewCompanyCustomer
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import javax.inject.Inject

/** Alta de empresa con RUC en primer ingreso (US11 E2). En línea. */
class RegisterCompanyCustomerUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(customer: NewCompanyCustomer): Result<Customer> {
        val invalid = when {
            !ReceptionValidation.isCompanyName(customer.companyName) -> "companyName"
            !ReceptionValidation.isCompanyTaxId(customer.taxId) -> "taxId"
            !ReceptionValidation.isOptionalEmail(customer.email) -> "email"
            !ReceptionValidation.isOptionalPhone(customer.phone) -> "phone"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidReceptionInputException(invalid))
        return receptionRepository.registerCompany(customer)
    }
}
