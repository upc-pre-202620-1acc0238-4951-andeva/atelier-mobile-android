package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import javax.inject.Inject

/** Al abrir Asistencia: perfil laboral, sedes, turno e historial (red → Room). Sin red se sigue con la caché. */
class RefreshAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke() = attendanceRepository.refresh()
}
