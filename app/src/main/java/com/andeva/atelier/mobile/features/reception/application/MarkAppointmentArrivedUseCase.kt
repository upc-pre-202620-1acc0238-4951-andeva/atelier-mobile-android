package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Registrar la llegada de una cita (US11 E1, US12 E1). El backend devuelve la cita y crea la orden en borrador
 * después, de forma asíncrona y con kilometraje 0 (H10 de docs/03-traceability.md): se busca hasta [ATTEMPTS] veces.
 * Con [ArrivalResult.WorkOrderReady] completar kilometraje y diagnóstico con `UpdateWorkOrderUseCase`; con
 * [ArrivalResult.WorkOrderPending] volver a buscar con `FindDraftWorkOrderForVehicleUseCase`. En línea.
 */
class MarkAppointmentArrivedUseCase @Inject constructor(
    private val receptionRepository: ReceptionRepository,
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(appointmentId: String): Result<ArrivalResult> {
        val appointment = receptionRepository.markArrived(appointmentId).getOrElse { return Result.failure(it) }
        repeat(ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RETRY_DELAY_MILLIS)
            val draft = workOrderRepository.findDraftForVehicle(appointment.branchId, appointment.vehicleId).getOrNull()
            if (draft != null) return Result.success(ArrivalResult.WorkOrderReady(appointment, draft.id))
        }
        return Result.success(ArrivalResult.WorkOrderPending(appointment))
    }

    companion object {
        const val ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 1_000L
    }
}
