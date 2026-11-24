package com.andeva.atelier.mobile.features.diagnostics.domain

import java.time.Instant

enum class PredictiveAlertType {
    ENGINE_OVERHEATING_RISK,
    BATTERY_FAILURE_RISK,
    CATALYTIC_SYSTEM_DEGRADATION,
    CYLINDER_MISFIRE_HAZARD,
    BRAKE_WEAR,
    OIL_LIFE_DEPLETION,
    EMISSIONS_FAILURE
}

enum class PredictiveAlertStatus { DISPATCHED, ACKNOWLEDGED, RESOLVED, DISMISSED }

/**
 * Alerta predictiva de un vehículo (`PredictiveAlertResponse`). No hay push de alertas al staff: se recarga al abrir
 * (docs/06-offline-sync.md §1). `recommendedServiceId` no se guarda en la caché, así que desde Room llega null.
 */
data class PredictiveAlert(
    val id: String,
    val vehicleId: String,
    val alertType: PredictiveAlertType?,
    val confidenceScore: Double,
    val message: String,
    val status: PredictiveAlertStatus?,
    val recommendedServiceId: String?,
    val createdAt: Instant?
)

/** Cita preventiva creada a partir de una alerta predictiva (`AppointmentResponse` de `docs/api/iot.md`). */
data class ConvertedAppointment(
    val appointmentId: String,
    val vehicleId: String,
    val customerId: String,
    val scheduledAt: Instant?,
    val status: String?,
    val notes: String?
)
