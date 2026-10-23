package com.andeva.atelier.mobile.core.data.remote

import com.andeva.atelier.mobile.core.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Agrega "Authorization: Bearer" (salvo en rutas públicas) y "Accept-Language: es". */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder().header("Accept-Language", "es")
        if (!PublicEndpoints.isPublic(original) && original.header("Authorization") == null) {
            val token = runBlocking { sessionManager.getAuthToken() }
            if (token != null) builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
