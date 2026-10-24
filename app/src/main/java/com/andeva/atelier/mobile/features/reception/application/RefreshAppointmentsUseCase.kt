package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Al abrir la agenda (US11): recarga las citas de la sede para [date] (día de Lima; hoy y mañana según 06 §4) y
 * la placa y el cliente que falten. Falla con `MissingBranchException` sin sede; sin red se sigue con la caché.
 */
class RefreshAppointmentsUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(date: LocalDate): Result<Unit> = receptionRepository.refreshAppointments(date)
}
