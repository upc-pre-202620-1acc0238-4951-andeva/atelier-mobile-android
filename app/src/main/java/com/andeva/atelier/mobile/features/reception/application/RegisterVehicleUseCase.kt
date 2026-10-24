package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.NewVehicle
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import javax.inject.Inject

/** Alta del vehículo con su titular (US11 E2, US44 E2). La placa se envía normalizada. En línea. */
class RegisterVehicleUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(vehicle: NewVehicle): Result<Vehicle> {
        val invalid = when {
            !ReceptionValidation.isPlate(vehicle.plate) -> "plate"
            !ReceptionValidation.isBrandOrModel(vehicle.brand) -> "brand"
            !ReceptionValidation.isBrandOrModel(vehicle.model) -> "model"
            !ReceptionValidation.isOptionalVin(vehicle.vin) -> "vin"
            !ReceptionValidation.isOptionalYear(vehicle.year) -> "year"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidReceptionInputException(invalid))
        return receptionRepository.registerVehicle(vehicle)
    }
}
