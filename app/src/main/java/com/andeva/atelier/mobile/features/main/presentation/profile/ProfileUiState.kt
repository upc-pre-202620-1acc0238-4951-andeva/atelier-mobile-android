package com.andeva.atelier.mobile.features.main.presentation.profile

/**
 * Cuenta de la sesión. La app no conoce el nombre del rol (solo sus permisos), así que no se muestra ninguno.
 * [planTier] es el nivel tal como lo envía el backend; vacío si la sesión no trae plan.
 */
data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val tenantName: String = "",
    val branchId: String? = null,
    val branchName: String? = null,
    val canChangeBranch: Boolean = false,
    val planTier: String = "",
    val iotTelemetryEnabled: Boolean = false,
    val aiDiagnosticsEnabled: Boolean = false,
    val pendingCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val confirmingSignOut: Boolean = false,
    val signingOut: Boolean = false
)
