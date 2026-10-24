package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import javax.inject.Inject

/** Ficha del cliente desde la caché `customers` (sin red). */
class ObserveCustomerUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    operator fun invoke(id: String) = receptionRepository.observeCustomer(id)
}
