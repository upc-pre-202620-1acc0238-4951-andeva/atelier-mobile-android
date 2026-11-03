package com.andeva.atelier.mobile.features.attendance.domain

class LocationOutsideGeofenceException(
    val distanceMeters: Double,
    val radiusMeters: Double
) : IllegalStateException("Ubicación fuera de la geocerca")
