package com.andeva.atelier.mobile.core.data.remote.session

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

interface DeviceTokenService {
    /** 204 sin contenido. `fcmToken = null` lo borra al cerrar sesión. */
    @PUT("api/v1/users/me/device-token")
    suspend fun register(@Body body: RegisterDeviceTokenRequestDto): Response<Unit>
}
