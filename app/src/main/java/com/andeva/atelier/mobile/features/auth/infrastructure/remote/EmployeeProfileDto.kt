package com.andeva.atelier.mobile.features.auth.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `EmployeeProfileResource` de docs/api/hr.md; tras el login solo se usan la sede y el turno. */
data class EmployeeProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("membershipId") val membershipId: String?,
    @SerializedName("assignedShiftId") val assignedShiftId: String?,
    @SerializedName("jobTitle") val jobTitle: String?,
    @SerializedName("employmentStatus") val employmentStatus: String?
)
