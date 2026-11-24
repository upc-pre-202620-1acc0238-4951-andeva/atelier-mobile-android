package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.ConvertedAppointment
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Convierte una alerta predictiva en cita preventiva de CRM con la fecha, hora, duración y motivo
 * acordados con el cliente (`POST /api/v1/iot/alerts/{id}/convert-to-appointment`, permiso `crm:appointments:create`).
 */
class ConvertAlertToAppointmentUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(
        alertId: String,
        scheduledAt: Instant? = null,
        estimatedDurationMinutes: Int? = null,
        reason: String? = null
    ): Result<ConvertedAppointment> =
        diagnosticsRepository.convertAlertToAppointment(alertId, scheduledAt, estimatedDurationMinutes, reason)
}
