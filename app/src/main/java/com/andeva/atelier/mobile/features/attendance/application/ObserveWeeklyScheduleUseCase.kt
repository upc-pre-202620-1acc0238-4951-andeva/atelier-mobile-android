package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import javax.inject.Inject

/** Mi turno de lunes a domingo, con sede, tolerancia y días de descanso (US09 E1). null sin turno asignado. */
class ObserveWeeklyScheduleUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    operator fun invoke() = attendanceRepository.observeSchedule()
}
