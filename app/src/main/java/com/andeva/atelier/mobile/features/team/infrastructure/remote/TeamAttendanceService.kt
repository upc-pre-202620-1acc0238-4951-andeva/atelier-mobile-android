package com.andeva.atelier.mobile.features.team.infrastructure.remote

import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Auditoría de asistencia y revisión de descargos de docs/api/hr.md. */
interface TeamAttendanceService {
    /** [date] `yyyy-MM-dd` (fecha de Lima). */
    @GET("api/v1/hr/attendances/branch/{branchId}/daily")
    suspend fun branchDaily(
        @Path("branchId") branchId: String,
        @Query("date") date: String
    ): Response<List<AttendanceDto>>

    /** [startDate] y [endDate] obligatorios, `yyyy-MM-dd`. */
    @GET("api/v1/hr/attendances/employee/{membershipId}/history")
    suspend fun history(
        @Path("membershipId") membershipId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<AttendanceDto>>

    @POST("api/v1/hr/attendances/{attendanceId}/justification/approve")
    suspend fun approveJustification(@Path("attendanceId") attendanceId: String): Response<AttendanceDto>

    @POST("api/v1/hr/attendances/{attendanceId}/justification/reject")
    suspend fun rejectJustification(@Path("attendanceId") attendanceId: String): Response<AttendanceDto>
}
