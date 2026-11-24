package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import java.time.Instant
import javax.inject.Inject

/** Reprogramar una cita existente (`POST /appointments/{id}/reschedule`, `crm:appointments:update`). En línea. */
class RescheduleAppointmentUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(appointmentId: String, newScheduledAt: Instant): Result<Appointment> {
        if (appointmentId.isBlank()) return Result.failure(InvalidReceptionInputException("appointmentId"))
        return receptionRepository.rescheduleAppointment(appointmentId, newScheduledAt)
    }
}
