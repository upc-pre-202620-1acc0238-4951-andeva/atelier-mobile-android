package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import javax.inject.Inject

/** Hidrata en Room los vehículos y clientes referenciados por órdenes de trabajo o citas cuando aún no están en caché. */
class EnrichReceptionReferencesUseCase @Inject constructor(
    private val repository: ReceptionRepository
) {
    suspend operator fun invoke(vehicleIds: Collection<String>, customerIds: Collection<String>) =
        repository.enrichReferences(vehicleIds.toList(), customerIds.toList())
}
