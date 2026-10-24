package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import javax.inject.Inject

/** Mi jornada de hoy, incluida una marcación que espera señal (US06, US08). */
class ObserveTodayAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    operator fun invoke() = attendanceRepository.observeToday()
}
