package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import javax.inject.Inject

/** Confirmar una cita pendiente (`POST /appointments/{id}/confirm`, `crm:appointments:update`). En línea. */
class ConfirmAppointmentUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(appointmentId: String): Result<Appointment> {
        if (appointmentId.isBlank()) return Result.failure(InvalidReceptionInputException("appointmentId"))
        return receptionRepository.confirmAppointment(appointmentId)
    }
}
