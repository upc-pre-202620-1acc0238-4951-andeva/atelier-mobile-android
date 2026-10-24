package com.andeva.atelier.mobile.features.attendance.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Lo que la app muestra de la geocerca. Es solo un aviso: la validación real la hace el backend (422). */
data class GeofenceCheck(
    val distanceMeters: Double,
    val isInside: Boolean,
    val isMock: Boolean
)

object Geofence {
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    /** Distancia de Haversine en metros. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    fun check(branch: Branch, location: DeviceLocation): GeofenceCheck {
        val distance = distanceMeters(branch.latitude, branch.longitude, location.latitude, location.longitude)
        return GeofenceCheck(distance, distance <= branch.geofenceRadiusMeters, location.isMock)
    }
}
