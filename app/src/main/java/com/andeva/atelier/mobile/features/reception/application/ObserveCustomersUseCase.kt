package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observa de forma reactiva la cartera de clientes del taller almacenada en Room. */
class ObserveCustomersUseCase @Inject constructor(
    private val repository: ReceptionRepository
) {
    operator fun invoke(): Flow<List<Customer>> = repository.observeCustomers()
}
