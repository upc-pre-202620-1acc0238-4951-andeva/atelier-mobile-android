package com.andeva.atelier.mobile.core.media

import android.content.Context
import com.google.gson.Gson
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupabaseUploaderTest {

    private val uploader = SupabaseUploader(OkHttpClient(), mockk<Context>(relaxed = true), Gson())

    @Test
    fun `lee el message del cuerpo de error de Storage`() {
        val body = """{"statusCode":"403","error":"Unauthorized","message":"new row violates row-level security policy"}"""
        assertEquals("new row violates row-level security policy", uploader.errorMessage(body))
    }

    @Test
    fun `sin message legible devuelve null`() {
        assertNull(uploader.errorMessage(""))
        assertNull(uploader.errorMessage("<html>Bad Gateway</html>"))
        assertNull(uploader.errorMessage("""{"message":""}"""))
    }
}
