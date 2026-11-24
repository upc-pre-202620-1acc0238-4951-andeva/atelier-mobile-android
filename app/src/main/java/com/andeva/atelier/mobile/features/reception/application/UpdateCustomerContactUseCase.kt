package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import javax.inject.Inject

/** Actualizar teléfono y correo de un cliente (`PUT /customers/{id}/contact`, `crm:customers:update`). En línea. */
class UpdateCustomerContactUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(customerId: String, email: String?, phone: String?): Result<Customer> {
        if (customerId.isBlank()) return Result.failure(InvalidReceptionInputException("customerId"))
        if (!ReceptionValidation.isOptionalEmail(email)) {
            return Result.failure(InvalidReceptionInputException("email"))
        }
        if (!ReceptionValidation.isOptionalPhone(phone)) {
            return Result.failure(InvalidReceptionInputException("phone"))
        }
        return receptionRepository.updateCustomerContact(customerId, email, phone)
    }
}
