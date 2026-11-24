package com.andeva.atelier.mobile.core.data.remote.tenant

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Perfil corporativo y fiscal del taller en sesión (`docs/api/iam.md`, US42). */
interface TenantService {
    @GET("api/v1/tenants/current")
    suspend fun getCurrent(): Response<TenantDto>

    @PUT("api/v1/tenants/current")
    suspend fun updateCurrent(@Body request: UpdateTenantProfileRequestDto): Response<TenantDto>
}
