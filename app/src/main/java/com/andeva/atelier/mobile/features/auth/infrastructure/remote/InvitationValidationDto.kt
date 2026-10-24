package com.andeva.atelier.mobile.features.auth.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `InvitationValidationResource` de docs/api/iam.md. */
data class InvitationValidationDto(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("email") val email: String?,
    @SerializedName("tenantName") val tenantName: String?,
    @SerializedName("roleName") val roleName: String?
)
