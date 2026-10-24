package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import java.time.LocalDate
import javax.inject.Inject

/** Citas de la sede para [date] desde Room, por hora, con placa y cliente si están en caché (US11). */
class ObserveAppointmentsUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    operator fun invoke(date: LocalDate) = receptionRepository.observeAppointments(date)
}
