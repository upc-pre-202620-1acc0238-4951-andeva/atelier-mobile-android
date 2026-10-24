package com.andeva.atelier.mobile.features.attendance.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de asistencia y turnos de docs/api/hr.md. */
interface AttendanceService {
    @POST("api/v1/hr/attendances/clock-in")
    suspend fun clockIn(@Body body: ClockInRequestDto): Response<AttendanceDto>

    @POST("api/v1/hr/attendances/clock-out")
    suspend fun clockOut(@Body body: ClockOutRequestDto): Response<AttendanceDto>

    /** Jornada activa de hoy. Sin jornada no está documentado qué responde (404 o vacío). */
    @GET("api/v1/hr/attendances/employee/{membershipId}/status-today")
    suspend fun statusToday(@Path("membershipId") membershipId: String): Response<AttendanceDto>

    /** [startDate] y [endDate] obligatorios, `yyyy-MM-dd` (fechas de Lima). */
    @GET("api/v1/hr/attendances/employee/{membershipId}/history")
    suspend fun history(
        @Path("membershipId") membershipId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<AttendanceDto>>

    @POST("api/v1/hr/attendances/{attendanceId}/justification")
    suspend fun submitJustification(
        @Path("attendanceId") attendanceId: String,
        @Body body: JustifyAttendanceRequestDto
    ): Response<AttendanceDto>

    @GET("api/v1/hr/work-shifts/{shiftId}")
    suspend fun getWorkShift(@Path("shiftId") shiftId: String): Response<WorkShiftDto>
}
