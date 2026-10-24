package com.andeva.atelier.mobile.core.data.remote.employee

import com.google.gson.annotations.SerializedName

/** `EmployeeProfileResource` de docs/api/hr.md; la app solo usa la sede y el turno. */
data class EmployeeProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("membershipId") val membershipId: String?,
    @SerializedName("assignedShiftId") val assignedShiftId: String?,
    @SerializedName("jobTitle") val jobTitle: String?,
    @SerializedName("employmentStatus") val employmentStatus: String?
)
