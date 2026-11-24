package com.andeva.atelier.mobile.core.data.remote.tenant

import com.google.gson.annotations.SerializedName

/** `TenantResource` de docs/api/iam.md (`GET/PUT /api/v1/tenants/current`). */
data class TenantDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("legalName") val legalName: String? = null,
    @SerializedName("taxId") val taxId: String? = null,
    @SerializedName("status") val status: String? = null
)

/** `UpdateTenantProfileResource` de docs/api/iam.md. */
data class UpdateTenantProfileRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("legalName") val legalName: String
)
