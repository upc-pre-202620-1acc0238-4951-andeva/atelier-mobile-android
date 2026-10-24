package com.andeva.atelier.mobile.features.attendance.infrastructure.remote

import retrofit2.Response
import retrofit2.http.GET

/** Sedes del taller con su geocerca (docs/api/iam.md). */
interface BranchService {
    @GET("api/v1/branches")
    suspend fun getBranches(): Response<List<BranchDto>>
}
