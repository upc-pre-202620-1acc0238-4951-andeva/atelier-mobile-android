package com.andeva.atelier.mobile.features.attendance.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `AttendanceResource` de docs/api/hr.md. */
data class AttendanceDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("membershipId") val membershipId: String?,
    @SerializedName("shiftId") val shiftId: String?,
    @SerializedName("clockIn") val clockIn: String?,
    @SerializedName("clockOut") val clockOut: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("distanceToBranchMeters") val distanceToBranchMeters: Double?,
    @SerializedName("justificationReason") val justificationReason: String?,
    @SerializedName("justificationStatus") val justificationStatus: String?,
    @SerializedName("workedMinutes") val workedMinutes: Long?,
    @SerializedName("regularMinutes") val regularMinutes: Long?,
    @SerializedName("overtimeMinutes") val overtimeMinutes: Long?
)
