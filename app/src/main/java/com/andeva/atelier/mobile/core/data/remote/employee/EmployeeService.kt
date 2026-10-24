package com.andeva.atelier.mobile.core.data.remote.employee

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/** Perfil laboral del colaborador. Lo usan `auth` (tras el login) y `attendance` (al refrescar). */
interface EmployeeService {
    /**
     * Sede (`branchId`) y turno (`assignedShiftId`) del colaborador. Tras el login se llama con el
     * token recién recibido en [authorization], antes de guardar la sesión: AuthInterceptor respeta
     * una cabecera `Authorization` explícita. Con `null` Retrofit omite la cabecera y AuthInterceptor
     * pone la de la sesión guardada.
     */
    @GET("api/v1/hr/employees/membership/{membershipId}")
    suspend fun getByMembership(
        @Header("Authorization") authorization: String?,
        @Path("membershipId") membershipId: String
    ): Response<EmployeeProfileDto>
}
