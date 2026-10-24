package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import javax.inject.Inject

/**
 * Buscar la unidad por placa en el counter (US11 E1): vehículo y titular. null = primer ingreso, seguir con el alta
 * (US11 E2). Sin red responde con la caché si la placa ya se vio.
 */
class FindVehicleByPlateUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(plate: String): Result<VehicleWithOwner?> {
        if (!ReceptionValidation.isPlate(plate)) return Result.failure(InvalidReceptionInputException("plate"))
        return receptionRepository.findVehicleByPlate(plate)
    }
}
