package com.andeva.atelier.mobile.features.attendance.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `WorkShiftResource` de docs/api/hr.md. Las horas llegan como `HH:mm:ss` de Lima. */
data class WorkShiftDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("gracePeriodMinutes") val gracePeriodMinutes: Int?,
    @SerializedName("isActive") val isActive: Boolean?,
    @SerializedName("workDays") val workDays: List<String>?,
    @SerializedName("restDays") val restDays: List<String>?
)
