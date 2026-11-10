package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observa de forma reactiva el padrón de vehículos registrados en la caché local del taller junto a su titular. */
class ObserveWorkshopVehiclesUseCase @Inject constructor(
    private val repository: ReceptionRepository
) {
    operator fun invoke(): Flow<List<VehicleWithOwner>> = repository.observeWorkshopVehicles()
}
