package com.andeva.atelier.mobile.core.data.remote

import android.content.Context
import com.andeva.atelier.mobile.core.domain.ApiException
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import javax.inject.Inject

/**
 * PDFs del backend (docs/04-mobile-integration.md §8): proforma, pase de salida, informe de salud, flota y flujo de
 * caja. Se descargan con el mismo `Authorization` y se guardan en `cacheDir/pdfs` (ruta del FileProvider de
 * `res/xml/file_paths.xml`). Abrirlo o compartirlo (`ACTION_VIEW` / `ACTION_SEND`) es de la UI.
 */
class PdfDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    /**
     * Ejecuta [call] (un endpoint `@Streaming` que devuelve `application/pdf`) y guarda el archivo con el nombre de
     * `Content-Disposition`, o [fallbackName] si no viene. Un error HTTP queda como [ApiException] con el `message`
     * del backend (p. ej. 422 del pase de salida de una orden sin pagar); sin red, la [IOException] original.
     */
    suspend fun download(fallbackName: String, call: suspend () -> Response<ResponseBody>): Result<File> = try {
        val response = call()
        val body = response.body()
        when {
            !response.isSuccessful -> Result.failure(response.toApiException(gson))
            body == null -> Result.failure(ApiException(response.code(), "EMPTY_BODY", "Respuesta vacía del servidor"))
            else -> {
                val name = fileName(response.headers()[CONTENT_DISPOSITION], fallbackName)
                Result.success(withContext(Dispatchers.IO) { save(body, name) })
            }
        }
    } catch (e: IOException) {
        Result.failure(e)
    }

    private fun save(body: ResponseBody, name: String): File {
        val dir = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        val target = File(dir, name)
        body.use { source -> target.outputStream().use { source.byteStream().copyTo(it) } }
        return target
    }

    companion object {
        const val DIRECTORY = "pdfs"
        private const val CONTENT_DISPOSITION = "Content-Disposition"

        /** `filename*=UTF-8''…` gana a `filename="…"`; el resultado es un nombre seguro que termina en `.pdf`. */
        fun fileName(contentDisposition: String?, fallbackName: String): String {
            val parts = contentDisposition.orEmpty().split(';').map { it.trim() }
            val extended = parts.firstOrNull { it.startsWith("filename*=", ignoreCase = true) }
                ?.substringAfter('=')?.substringAfter("''")
                ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            val plain = parts.firstOrNull { it.startsWith("filename=", ignoreCase = true) }
                ?.substringAfter('=')?.trim('"')
            val raw = (extended ?: plain)?.substringAfterLast('/')?.substringAfterLast('\\')
                ?.takeIf { it.isNotBlank() } ?: fallbackName
            val safe = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
            return if (safe.endsWith(".pdf", ignoreCase = true)) safe else "$safe.pdf"
        }
    }
}
