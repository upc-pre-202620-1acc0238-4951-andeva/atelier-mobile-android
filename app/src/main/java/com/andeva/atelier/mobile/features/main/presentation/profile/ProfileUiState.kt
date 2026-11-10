package com.andeva.atelier.mobile.features.main.presentation.profile

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val tenantName: String = "",
    val branchId: String? = null,
    val branchName: String? = null,
    val roleTitle: String = "Colaborador de taller",
    val planName: String = "",
    val iotTelemetryEnabled: Boolean = false,
    val aiDiagnosticsEnabled: Boolean = false,
    val permissionsCount: Int = 0,
    val permissions: List<String> = emptyList(),
    val membershipId: String? = null,
    val pendingCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val appVersion: String = "1.0.0",
    val confirmingSignOut: Boolean = false,
    val signingOut: Boolean = false
) {
    /** Iniciales del avatar: primera letra de las dos primeras palabras del nombre. */
    val initials: String
        get() = fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}
