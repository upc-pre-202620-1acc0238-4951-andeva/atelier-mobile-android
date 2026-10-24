package com.andeva.atelier.mobile.features.attendance.domain

/** Sede con su geocerca (`BranchResource` de docs/api/iam.md). */
data class Branch(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadiusMeters: Double,
    val isActive: Boolean
)
