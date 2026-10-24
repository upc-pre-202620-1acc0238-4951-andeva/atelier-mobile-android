package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.NewIndividualCustomer
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import javax.inject.Inject

/** Alta de persona natural en primer ingreso o walk-in (US11 E2, US44 E2). En línea. */
class RegisterIndividualCustomerUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(customer: NewIndividualCustomer): Result<Customer> {
        val invalid = when {
            !ReceptionValidation.isName(customer.firstName) -> "firstName"
            !ReceptionValidation.isName(customer.lastName) -> "lastName"
            !ReceptionValidation.isPersonTaxId(customer.taxId) -> "taxId"
            !ReceptionValidation.isOptionalEmail(customer.email) -> "email"
            !ReceptionValidation.isOptionalPhone(customer.phone) -> "phone"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidReceptionInputException(invalid))
        return receptionRepository.registerIndividual(customer)
    }
}
