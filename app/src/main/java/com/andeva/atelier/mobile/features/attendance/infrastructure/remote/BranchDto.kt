package com.andeva.atelier.mobile.features.attendance.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `BranchResource` de docs/api/iam.md. */
data class BranchDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("geofenceRadiusMeters") val geofenceRadiusMeters: Int?,
    @SerializedName("isActive") val isActive: Boolean?
)
