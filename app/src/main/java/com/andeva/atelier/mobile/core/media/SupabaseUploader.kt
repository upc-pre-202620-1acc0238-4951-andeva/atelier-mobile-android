package com.andeva.atelier.mobile.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.andeva.atelier.mobile.BuildConfig
import com.andeva.atelier.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.andeva.atelier.mobile.core.di.SupabaseClient
import com.andeva.atelier.mobile.core.domain.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Subida directa a Supabase Storage (docs/04-mobile-integration.md §3). El backend no recibe archivos:
 * solo la URL pública, que debe ser https, del host de Supabase y del bucket configurado.
 */
@Singleton
class SupabaseUploader @Inject constructor(
    @SupabaseClient private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val bucket = BuildConfig.SUPABASE_BUCKET

    /** Determinista: se puede armar el payload de `ATTACH_*` antes de subir la foto. */
    fun publicUrlFor(storagePath: String): String = "$baseUrl/storage/v1/object/public/$bucket/$storagePath"

    /** Sube [file] a [storagePath] con `x-upsert` (reintentar la misma ruta reemplaza el mismo objeto). */
    suspend fun upload(file: File, storagePath: String): Result<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            return@withContext Result.failure(
                ApiException(0, "SUPABASE_NOT_CONFIGURED", context.getString(R.string.error_storage_not_configured))
            )
        }
        val request = Request.Builder()
            .url("$baseUrl/storage/v1/object/$bucket/$storagePath")
            .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("x-upsert", "true")
            .post(file.asRequestBody(JPEG))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(publicUrlFor(storagePath))
                } else {
                    Result.failure(ApiException(response.code, "HTTP_${response.code}", response.message.ifBlank { "Error ${response.code}" }))
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /** JPEG con el lado mayor en 1600 px y calidad 80, antes de guardarlo en `filesDir/pending`. */
    suspend fun compress(source: File, target: File): File = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Cannot decode image: ${source.path}")
        val scale = MAX_SIDE_PX.toFloat() / max(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else {
            decoded
        }
        target.parentFile?.mkdirs()
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        target
    }

    private companion object {
        const val MAX_SIDE_PX = 1600
        const val JPEG_QUALITY = 80
        val JPEG = "image/jpeg".toMediaType()
    }
}
