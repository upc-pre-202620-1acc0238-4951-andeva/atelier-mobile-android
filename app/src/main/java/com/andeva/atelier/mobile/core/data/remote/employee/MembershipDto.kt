package com.andeva.atelier.mobile.core.data.remote.employee

import com.google.gson.annotations.SerializedName

/**
 * `MembershipResource` de docs/api/iam.md.
 * Utilizado para resolver el nombre comercial ([employeeName]) de los colaboradores por su membresía.
 */
data class MembershipDto(
    @SerializedName("id") val id: String,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("status") val status: String?
)
