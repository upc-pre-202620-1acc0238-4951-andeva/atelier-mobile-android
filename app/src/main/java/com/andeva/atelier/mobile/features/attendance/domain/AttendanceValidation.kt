package com.andeva.atelier.mobile.features.attendance.domain

/**
 * Restricciones de docs/api/hr.md y docs/04-mobile-integration.md §7. Solo evitan una llamada
 * inútil; el backend vuelve a validar todo.
 */
object AttendanceValidation {
    const val JUSTIFICATION_MAX = 500

    /** Peor que esto, pedir salir a cielo abierto antes de marcar. */
    const val MAX_ACCURACY_METERS = 50f

    fun isJustification(value: String) = value.isNotBlank() && value.trim().length <= JUSTIFICATION_MAX

    fun isAccurate(location: DeviceLocation) = location.accuracyMeters <= MAX_ACCURACY_METERS
}
