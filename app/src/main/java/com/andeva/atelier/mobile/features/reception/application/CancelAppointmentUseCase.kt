package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import javax.inject.Inject

/** Cancelar una cita registrando el motivo (`POST /appointments/{id}/cancel`, `crm:appointments:update`). En línea. */
class CancelAppointmentUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(appointmentId: String, cancellationReason: String): Result<Appointment> {
        if (appointmentId.isBlank()) return Result.failure(InvalidReceptionInputException("appointmentId"))
        if (!ReceptionValidation.isAppointmentCancelReason(cancellationReason)) {
            return Result.failure(InvalidReceptionInputException("cancellationReason"))
        }
        return receptionRepository.cancelAppointment(appointmentId, cancellationReason.trim())
    }
}
