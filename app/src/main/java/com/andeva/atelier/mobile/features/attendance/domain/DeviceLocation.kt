package com.andeva.atelier.mobile.features.attendance.domain

/** Ubicación fresca del teléfono. [isMock]: ubicación simulada (el backend no lo verifica). */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val isMock: Boolean
)
