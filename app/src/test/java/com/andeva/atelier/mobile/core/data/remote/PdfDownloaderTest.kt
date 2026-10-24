package com.andeva.atelier.mobile.core.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.domain.ApiException
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfDownloaderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val downloader = PdfDownloader(context, Gson())
    private val pdf = "application/pdf".toMediaType()

    @Test
    fun `guarda el pdf en cacheDir pdfs con el nombre de Content-Disposition`() = runTest {
        val bytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)

        val file = downloader.download("proforma") {
            Response.success(bytes.toResponseBody(pdf), headersOf("Content-Disposition", "attachment; filename=\"proforma-OT-15.pdf\""))
        }.getOrThrow()

        assertEquals(File(context.cacheDir, "pdfs/proforma-OT-15.pdf"), file)
        assertArrayEquals(bytes, file.readBytes())
    }

    @Test
    fun `un error trae el message del backend y sin red la IOException`() = runTest {
        val error = downloader.download("pase") {
            Response.error(422, """{"code":"UNPROCESSABLE_ENTITY","message":"La orden no está pagada","details":[]}""".toResponseBody())
        }.exceptionOrNull() as ApiException

        assertEquals(422, error.httpStatus)
        assertEquals("La orden no está pagada", error.message)
        assertTrue(downloader.download("pase") { throw IOException("sin red") }.exceptionOrNull() is IOException)
    }

    @Test
    fun `el nombre del archivo es seguro y siempre termina en pdf`() {
        assertEquals("informe_salud.pdf", PdfDownloader.fileName("attachment; filename*=UTF-8''informe%20salud.pdf", "x"))
        assertEquals("evil.pdf", PdfDownloader.fileName("attachment; filename=\"../../evil.pdf\"", "x"))
        assertEquals("proforma-wo1.pdf", PdfDownloader.fileName(null, "proforma-wo1"))
    }
}
