package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import javax.inject.Inject

/** Ficha técnica del vehículo desde la caché `vehicles` (sin red). */
class ObserveVehicleUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    operator fun invoke(id: String) = receptionRepository.observeVehicle(id)
}
