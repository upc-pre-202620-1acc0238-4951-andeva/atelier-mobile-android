package com.andeva.atelier.mobile.features.attendance.domain

/**
 * Ubicación para la geocerca (docs/04-mobile-integration.md §7). Falla con
 * [LocationPermissionDeniedException] o [LocationUnavailableException]; pedir el permiso es de la UI.
 */
interface LocationProvider {
    suspend fun currentLocation(): Result<DeviceLocation>
}
