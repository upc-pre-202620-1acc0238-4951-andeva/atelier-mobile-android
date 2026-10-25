package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Fallas activas del vehículo desde Room, incluidas las que aún están en cola (`isPending`). */
class ObserveVehicleFaultsUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    operator fun invoke(vehicleId: String): Flow<List<VehicleFault>> = diagnosticsRepository.observeActiveFaults(vehicleId)
}
