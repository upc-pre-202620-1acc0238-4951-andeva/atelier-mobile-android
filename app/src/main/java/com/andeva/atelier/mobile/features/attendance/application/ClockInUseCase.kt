package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceValidation
import com.andeva.atelier.mobile.features.attendance.domain.BranchGeofenceUnavailableException
import com.andeva.atelier.mobile.features.attendance.domain.ClockInResult
import com.andeva.atelier.mobile.features.attendance.domain.Geofence
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.LocationTooInaccurateException
import com.andeva.atelier.mobile.features.attendance.domain.MissingAssignmentException
import com.andeva.atelier.mobile.features.attendance.domain.LocationOutsideGeofenceException
import com.andeva.atelier.mobile.features.attendance.domain.MockLocationException
import javax.inject.Inject

/**
 * US06: obtiene una fijación GPS fresca y valida la geocerca de la sede asignada con Haversine.
 * La misma validación vuelve a ejecutarse en el backend; la caché permite aplicar el bloqueo
 * también sin conectividad. No se registra ni encola una ubicación no verificable.
 */
class ClockInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val locationProvider: LocationProvider
) {
    suspend operator fun invoke(branchId: String?, shiftId: String?): Result<ClockInResult> {
        if (branchId.isNullOrBlank() || shiftId.isNullOrBlank()) {
            return Result.failure(MissingAssignmentException())
        }

        val location = locationProvider.currentLocation().getOrElse { return Result.failure(it) }
        if (!AttendanceValidation.isAccurate(location)) {
            return Result.failure(LocationTooInaccurateException(location.accuracyMeters))
        }
        if (location.isMock) return Result.failure(MockLocationException())

        val branch = attendanceRepository.findBranch(branchId)
            ?: return Result.failure(BranchGeofenceUnavailableException())
        val geofence = Geofence.check(branch, location)
        if (!geofence.isInside) {
            return Result.failure(LocationOutsideGeofenceException(geofence.distanceMeters, branch.geofenceRadiusMeters))
        }

        return attendanceRepository.clockIn(branchId, shiftId, location)
            .map { ClockInResult(it, geofence) }
    }
}
