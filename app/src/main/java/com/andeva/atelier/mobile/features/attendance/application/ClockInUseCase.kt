package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceValidation
import com.andeva.atelier.mobile.features.attendance.domain.ClockInResult
import com.andeva.atelier.mobile.features.attendance.domain.Geofence
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.LocationTooInaccurateException
import com.andeva.atelier.mobile.features.attendance.domain.MissingAssignmentException
import javax.inject.Inject

/**
 * Marcar ingreso (US06). Ubicación fresca con precisión ≤ 50 m; la distancia a la sede es solo un
 * aviso, porque la geocerca la valida el backend (422 con la distancia en `message`). Sin red se
 * encola con la hora del dispositivo.
 */
class ClockInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val locationProvider: LocationProvider
) {
    suspend operator fun invoke(branchId: String?, shiftId: String?): Result<ClockInResult> {
        if (branchId.isNullOrBlank() || shiftId.isNullOrBlank()) return Result.failure(MissingAssignmentException())
        val location = locationProvider.currentLocation().getOrElse { return Result.failure(it) }
        if (!AttendanceValidation.isAccurate(location)) return Result.failure(LocationTooInaccurateException(location.accuracyMeters))

        val geofence = attendanceRepository.findBranch(branchId)?.let { Geofence.check(it, location) }
        return attendanceRepository.clockIn(branchId, shiftId, location).map { ClockInResult(it, geofence) }
    }
}
