package com.andeva.atelier.mobile.core.data.remote

import android.content.Context
import com.andeva.atelier.mobile.BuildConfig
import com.andeva.atelier.mobile.core.di.SupabaseClient
import com.andeva.atelier.mobile.core.domain.ApiException
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Descarga documentos del backend y del PSE (docs/04-mobile-integration.md §8): proforma, pase de salida,
 * informe de salud, flota, flujo de caja y comprobantes SUNAT (PDF, XML, CDR ZIP). Se guardan en
 * `cacheDir/pdfs` (ruta del FileProvider de `res/xml/file_paths.xml`).
 */
class PdfDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val authenticatedClient: OkHttpClient = OkHttpClient(),
    @SupabaseClient private val externalClient: OkHttpClient = OkHttpClient()
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
                val name = fileName(response.headers()[CONTENT_DISPOSITION], fallbackName, "pdf")
                Result.success(withContext(Dispatchers.IO) { save(body, name) })
            }
        }
    } catch (e: IOException) {
        Result.failure(e)
    }

    /**
     * Descarga un documento desde [url] absoluta (p. ej. `pdfUrl`, `xmlUrl` o `cdrUrl` de SUNAT/PSE).
     * Exige esquema `https://` y envía la cabecera `Authorization` **únicamente** cuando `(scheme, host, port)`
     * coincide exactamente con [BuildConfig.API_BASE_URL]; cualquier host externo usa [externalClient] sin JWT.
     */
    suspend fun downloadFromUrl(
        url: String,
        fallbackName: String,
        extension: String = "pdf",
        apiBaseUrl: String = BuildConfig.API_BASE_URL
    ): Result<File> = withContext(Dispatchers.IO) {
        val parsed = url.trim().toHttpUrlOrNull()
        if (parsed == null || !parsed.isHttps) {
            return@withContext Result.failure(
                ApiException(400, "INVALID_DOCUMENT_URL", "El enlace del documento no es una dirección HTTPS válida")
            )
        }
        val apiUrl = apiBaseUrl.toHttpUrlOrNull()
        val isSameBackendHost = apiUrl != null &&
            parsed.scheme.equals(apiUrl.scheme, ignoreCase = true) &&
            parsed.host.equals(apiUrl.host, ignoreCase = true) &&
            parsed.port == apiUrl.port
        val client = if (isSameBackendHost) authenticatedClient else externalClient
        try {
            val request = Request.Builder().url(parsed).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body
                when {
                    !response.isSuccessful -> Result.failure(
                        ApiException(
                            httpStatus = response.code,
                            code = "DOCUMENT_DOWNLOAD_FAILED",
                            message = "No se pudo descargar el documento (HTTP ${response.code})"
                        )
                    )
                    body == null -> Result.failure(
                        ApiException(response.code, "EMPTY_BODY", "Respuesta vacía al descargar el documento")
                    )
                    else -> {
                        val name = fileName(response.header(CONTENT_DISPOSITION), fallbackName, extension)
                        Result.success(save(body, name))
                    }
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
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

        /** `filename*=UTF-8''…` gana a `filename="…"`; garantiza un nombre seguro con la extensión esperada. */
        fun fileName(contentDisposition: String?, fallbackName: String, extension: String = "pdf"): String {
            val parts = contentDisposition.orEmpty().split(';').map { it.trim() }
            val extended = parts.firstOrNull { it.startsWith("filename*=", ignoreCase = true) }
                ?.substringAfter('=')?.substringAfter("''")
                ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            val plain = parts.firstOrNull { it.startsWith("filename=", ignoreCase = true) }
                ?.substringAfter('=')?.trim('"')
            val raw = (extended ?: plain)?.substringAfterLast('/')?.substringAfterLast('\\')
                ?.takeIf { it.isNotBlank() } ?: fallbackName
            val safe = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val ext = extension.trimStart('.').ifBlank { "pdf" }
            return if (safe.endsWith(".$ext", ignoreCase = true)) safe else "$safe.$ext"
        }
    }
}
