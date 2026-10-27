package com.andeva.atelier.mobile.features.auth.domain

/** Sede que una cuenta sin perfil laboral puede elegir (`BranchResource` de docs/api/iam.md). */
data class BranchOption(
    val id: String,
    val name: String,
    /** Radio de la geocerca de marcación; null si el backend no lo informa. */
    val geofenceRadiusMeters: Int?
)
