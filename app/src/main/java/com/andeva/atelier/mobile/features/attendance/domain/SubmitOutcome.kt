package com.andeva.atelier.mobile.features.attendance.domain

/** Resultado de una acción que funciona sin red (marcar ingreso o salida, enviar descargo). */
sealed interface SubmitOutcome {
    /** Con red: el backend ya la registró. */
    data class Recorded(val attendance: Attendance) : SubmitOutcome

    /** Sin red: quedó en la cola y se envía al recuperar señal. */
    data object Queued : SubmitOutcome

    /** El mismo hecho ya estaba en la cola (pendiente o fallido: ver "Pendientes de sincronizar"). */
    data object AlreadyQueued : SubmitOutcome
}

/** Marcación de ingreso con el aviso de geocerca; [geofence] es null si la sede no está en caché. */
data class ClockInResult(
    val outcome: SubmitOutcome,
    val geofence: GeofenceCheck?
)
