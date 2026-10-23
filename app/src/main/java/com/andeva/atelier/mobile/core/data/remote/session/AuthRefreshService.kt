package com.andeva.atelier.mobile.core.data.remote.session

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoints públicos de sesión. Se crea con el cliente `@RefreshClient` (sin TokenAuthenticator). */
interface AuthRefreshService {
    /** Síncrono a propósito: lo llama TokenAuthenticator desde un hilo de OkHttp. */
    @POST("api/v1/auth/refresh")
    fun refresh(@Body body: RefreshTokenRequestDto): Call<AuthenticatedUserDto>

    @POST("api/v1/auth/sign-out")
    suspend fun signOut(@Body body: RefreshTokenRequestDto): Response<MessageResponseDto>
}
