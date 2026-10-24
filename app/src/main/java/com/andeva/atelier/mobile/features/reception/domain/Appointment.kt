package com.andeva.atelier.mobile.features.reception.domain

import java.time.Instant

/** `AppointmentStatus` de docs/api/README.md §8. */
enum class AppointmentStatus { PENDING, CONFIRMED, ARRIVED, CANCELED }

/**
 * Cita de la agenda de la sede (`AppointmentResource`). El backend solo manda ids: [plate] y [customerName]
 * salen de las cachés `vehicles` y `customers` y son null hasta que se carguen (docs/06-offline-sync.md §1).
 */
data class Appointment(
    val id: String,
    val branchId: String,
    val customerId: String,
    val vehicleId: String,
    val scheduledAt: Instant,
    val estimatedDurationMinutes: Int?,
    val reason: String?,
    val status: AppointmentStatus?,
    val plate: String? = null,
    val customerName: String? = null
)

/**
 * Llegada de una cita (US11 E1, H10 de docs/03-traceability.md). El backend crea la orden en borrador después, de
 * forma asíncrona: [WorkOrderReady] si ya apareció, [WorkOrderPending] si aún no (volver a buscar luego).
 */
sealed interface ArrivalResult {
    val appointment: Appointment

    data class WorkOrderReady(override val appointment: Appointment, val workOrderId: String) : ArrivalResult

    data class WorkOrderPending(override val appointment: Appointment) : ArrivalResult
}
