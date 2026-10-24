package com.andeva.atelier.mobile.features.attendance.domain

import java.time.Instant

/**
 * Estado de mi jornada de hoy: combina la caché con la cola, así la UI no deja marcar dos veces
 * mientras una marcación espera señal.
 */
sealed interface TodayAttendance {
    data object NotClockedIn : TodayAttendance

    /** Ingreso guardado sin red: se enviará al recuperar señal. */
    data class ClockInQueued(val capturedAt: Instant) : TodayAttendance

    data class ClockedIn(val attendance: Attendance) : TodayAttendance

    /** Salida guardada sin red. [attendance] es null si el ingreso también sigue en cola. */
    data class ClockOutQueued(val attendance: Attendance?, val capturedAt: Instant) : TodayAttendance

    data class Completed(val attendance: Attendance) : TodayAttendance
}
