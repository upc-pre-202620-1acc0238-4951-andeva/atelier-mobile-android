package com.andeva.atelier.mobile.core.data.remote

import com.andeva.atelier.mobile.core.domain.ApiException
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ApiResultTest {

    private val gson = Gson()
    private val json = "application/json".toMediaType()

    @Test
    fun `2xx con cuerpo se mapea`() = runTest {
        val result = apiCall(gson, { Response.success("hola") }) { it.length }
        assertEquals(4, result.getOrThrow())
    }

    @Test
    fun `204 sin cuerpo es exito con apiCallUnit`() = runTest {
        val result = apiCallUnit(gson) { Response.success<Unit>(204, null) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `error con formato unico conserva code, message y details`() = runTest {
        val body = """{"code":"VALIDATION_FAILED","message":"La solicitud contiene datos inválidos",
            "details":["latitude: must be less than or equal to 90"],"timestamp":"2026-10-20T15:04:05Z"}"""
        val result = apiCall(gson, { Response.error<String>(400, body.toResponseBody(json)) }) { it }
        val error = result.exceptionOrNull() as ApiException
        assertEquals(400, error.httpStatus)
        assertEquals("VALIDATION_FAILED", error.code)
        assertEquals("La solicitud contiene datos inválidos", error.message)
        assertEquals(listOf("latitude: must be less than or equal to 90"), error.details)
    }

    @Test
    fun `error sin cuerpo JSON usa el codigo HTTP`() = runTest {
        val result = apiCallUnit(gson) { Response.error<Unit>(502, "<html>Bad gateway</html>".toResponseBody(null)) }
        val error = result.exceptionOrNull() as ApiException
        assertEquals(502, error.httpStatus)
        assertEquals("HTTP_502", error.code)
    }

    @Test
    fun `sin red queda la IOException`() = runTest {
        val result = apiCall<String, String>(gson, { throw IOException("timeout") }) { it }
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
