package com.andeva.atelier.mobile.core.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Reintento acotado (1 vez) exclusivamente para peticiones idempotentes `GET` y `HEAD` ante fallos de socket
 * previos a respuesta (keep-alive cerrado por Render o salto Wi-Fi/4G). Los `POST`, `PUT`, `PATCH` y `DELETE`
 * nunca se reintentan en capa de transporte (protege comprobantes SUNAT, cobros y rotación de refresh token, H28).
 */
class IdempotentGetRetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" && request.method != "HEAD") {
            return chain.proceed(request)
        }
        return try {
            chain.proceed(request)
        } catch (firstError: IOException) {
            if (chain.call().isCanceled()) throw firstError
            chain.proceed(request)
        }
    }
}
