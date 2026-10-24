package com.andeva.atelier.mobile.features.auth.domain

/** Datos para `POST /invitations/accept` (`AcceptInvitationResource`). */
data class AcceptInvitation(
    val token: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val phone: String?
)
