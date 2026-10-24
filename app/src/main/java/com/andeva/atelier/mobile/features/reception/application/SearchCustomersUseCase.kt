package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import javax.inject.Inject

/** Buscar al cliente por documento o nombre (US11 E1). Sin red busca en los clientes ya vistos. */
class SearchCustomersUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(query: String): Result<List<Customer>> = receptionRepository.searchCustomers(query)
}
