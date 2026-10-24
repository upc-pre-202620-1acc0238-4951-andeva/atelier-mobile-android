package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import javax.inject.Inject

/**
 * Marcar salida (US08). El backend calcula minutos trabajados, regulares y sobretiempo, y pausa las
 * tareas en curso. Sin red se encola con la hora del dispositivo.
 */
class ClockOutUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke() = attendanceRepository.clockOut()
}
