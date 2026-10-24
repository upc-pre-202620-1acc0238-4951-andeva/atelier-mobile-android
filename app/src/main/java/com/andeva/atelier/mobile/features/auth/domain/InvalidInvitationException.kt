package com.andeva.atelier.mobile.features.auth.domain

/** `GET /invitations/validate` respondió 200 con `valid = false`. */
class InvalidInvitationException : Exception("Invitation is not valid")
