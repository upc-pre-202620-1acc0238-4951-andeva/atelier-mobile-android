package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import javax.inject.Inject

/** Vehículos con titularidad vigente del cliente (US11 E1, al buscar por documento). En línea. */
class GetCustomerVehiclesUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(customerId: String): Result<List<Vehicle>> = receptionRepository.getCustomerVehicles(customerId)
}
