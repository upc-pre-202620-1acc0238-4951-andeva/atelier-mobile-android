package com.andeva.atelier.mobile.features.auth.infrastructure.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface EmployeeService {
    /**
     * Sede (`branchId`) y turno (`assignedShiftId`) del colaborador. Se llama tras el login con el
     * token recién recibido en [authorization], antes de guardar la sesión: AuthInterceptor respeta
     * una cabecera `Authorization` explícita.
     */
    @GET("api/v1/hr/employees/membership/{membershipId}")
    suspend fun getByMembership(
        @Header("Authorization") authorization: String,
        @Path("membershipId") membershipId: String
    ): Response<EmployeeProfileDto>
}
