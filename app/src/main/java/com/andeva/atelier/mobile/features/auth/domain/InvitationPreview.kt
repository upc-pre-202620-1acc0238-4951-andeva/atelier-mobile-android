package com.andeva.atelier.mobile.features.auth.domain

/** Resultado de `GET /invitations/validate`: a qué taller y con qué rol invitan. */
data class InvitationPreview(
    val email: String,
    val tenantName: String,
    val roleName: String
)
