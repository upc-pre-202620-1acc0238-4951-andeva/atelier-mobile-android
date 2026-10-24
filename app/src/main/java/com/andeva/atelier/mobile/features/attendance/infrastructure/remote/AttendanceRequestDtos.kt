package com.andeva.atelier.mobile.features.attendance.infrastructure.remote

import com.google.gson.annotations.SerializedName

/**
 * `ClockInRequest`. [clockInTime] (ISO-8601 UTC) solo va en la marcación encolada sin red; con red
 * se omite y el backend usa su hora. Gson no serializa los null.
 */
data class ClockInRequestDto(
    @SerializedName("branchId") val branchId: String,
    @SerializedName("shiftId") val shiftId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("clockInTime") val clockInTime: String? = null
)

/** `ClockOutRequest`: sin [attendanceId] el backend cierra la jornada abierta. */
data class ClockOutRequestDto(
    @SerializedName("attendanceId") val attendanceId: String? = null,
    @SerializedName("clockOutTime") val clockOutTime: String? = null
)

/** `JustifyAttendanceRequest`. */
data class JustifyAttendanceRequestDto(
    @SerializedName("reason") val reason: String
)

/** Payload de `SUBMIT_JUSTIFICATION`: el body más el id de la ruta. */
data class JustificationPayload(
    @SerializedName("attendanceId") val attendanceId: String,
    @SerializedName("reason") val reason: String
)
