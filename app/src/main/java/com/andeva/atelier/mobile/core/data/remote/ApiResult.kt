package com.andeva.atelier.mobile.core.data.remote

import com.andeva.atelier.mobile.core.domain.ApiException
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Mapea `Response<T>` a `Result<R>`. Un error HTTP queda como [ApiException] con el `message`
 * del backend; sin red queda la [IOException] original.
 */
suspend fun <T, R> apiCall(gson: Gson, call: suspend () -> Response<T>, map: (T) -> R): Result<R> = try {
    val response = call()
    val body = response.body()
    if (response.isSuccessful && body != null) {
        Result.success(map(body))
    } else if (response.isSuccessful) {
        Result.failure(ApiException(response.code(), "EMPTY_BODY", "Respuesta vacía del servidor"))
    } else {
        Result.failure(response.toApiException(gson))
    }
} catch (e: IOException) {
    Result.failure(e)
}

/** Para endpoints sin cuerpo de respuesta (por ejemplo 204 de `PUT /users/me/device-token`). */
suspend fun apiCallUnit(gson: Gson, call: suspend () -> Response<*>): Result<Unit> = try {
    val response = call()
    if (response.isSuccessful) Result.success(Unit) else Result.failure(response.toApiException(gson))
} catch (e: IOException) {
    Result.failure(e)
}

fun Response<*>.toApiException(gson: Gson): ApiException {
    val error = runCatching {
        errorBody()?.charStream()?.use { gson.fromJson(it, ErrorResponseDto::class.java) }
    }.getOrNull()
    return ApiException(
        httpStatus = code(),
        code = error?.code ?: "HTTP_${code()}",
        message = error?.message ?: "Error ${code()}",
        details = error?.details.orEmpty()
    )
}
