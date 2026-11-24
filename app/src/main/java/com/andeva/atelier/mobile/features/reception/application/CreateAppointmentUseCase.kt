package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import java.time.Instant
import javax.inject.Inject

/** Agendar una cita en la sede de la sesión (`POST /appointments`, `crm:appointments:create`). En línea. */
class CreateAppointmentUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(
        customerId: String,
        vehicleId: String,
        scheduledAt: Instant,
        estimatedDurationMinutes: Int? = null,
        reason: String? = null
    ): Result<Appointment> {
        if (customerId.isBlank()) return Result.failure(InvalidReceptionInputException("customerId"))
        if (vehicleId.isBlank()) return Result.failure(InvalidReceptionInputException("vehicleId"))
        if (!ReceptionValidation.isOptionalDurationMinutes(estimatedDurationMinutes)) {
            return Result.failure(InvalidReceptionInputException("estimatedDurationMinutes"))
        }
        if (!ReceptionValidation.isOptionalAppointmentReason(reason)) {
            return Result.failure(InvalidReceptionInputException("reason"))
        }
        return receptionRepository.createAppointment(
            customerId = customerId,
            vehicleId = vehicleId,
            scheduledAt = scheduledAt,
            estimatedDurationMinutes = estimatedDurationMinutes,
            reason = reason
        )
    }
}
