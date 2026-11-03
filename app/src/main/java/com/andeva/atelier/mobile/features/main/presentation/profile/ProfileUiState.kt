package com.andeva.atelier.mobile.features.main.presentation.profile

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val tenantName: String = "",
    val pendingCount: Int = 0,
    val confirmingSignOut: Boolean = false,
    val signingOut: Boolean = false
) {
    /** Iniciales del avatar: primera letra de las dos primeras palabras del nombre. */
    val initials: String
        get() = fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}
