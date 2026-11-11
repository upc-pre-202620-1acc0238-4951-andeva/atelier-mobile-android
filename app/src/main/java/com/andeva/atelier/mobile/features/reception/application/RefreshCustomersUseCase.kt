package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import javax.inject.Inject

/** Recarga la cartera de clientes del taller (`GET /api/v1/customers`) y la guarda en Room. */
class RefreshCustomersUseCase @Inject constructor(
    private val repository: ReceptionRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<Unit> = repository.refreshCustomers(force)
}
