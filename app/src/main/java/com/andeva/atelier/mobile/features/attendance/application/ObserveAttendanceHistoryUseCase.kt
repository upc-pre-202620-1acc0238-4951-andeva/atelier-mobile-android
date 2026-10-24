package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import javax.inject.Inject

/** Mis asistencias de los últimos 30 días, con minutos trabajados y estado del descargo (US06–US08, US10). */
class ObserveAttendanceHistoryUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    operator fun invoke() = attendanceRepository.observeHistory()
}
