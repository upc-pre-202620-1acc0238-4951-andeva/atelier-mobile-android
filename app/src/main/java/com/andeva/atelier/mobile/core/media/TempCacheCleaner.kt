package com.andeva.atelier.mobile.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Purga archivos temporales sensibles (`cacheDir/pdfs` y `cacheDir/capture`) al cerrar sesión (US04). */
interface TempCacheCleaner {
    fun clearSensitiveTempFiles()
}

object NoOpTempCacheCleaner : TempCacheCleaner {
    override fun clearSensitiveTempFiles() = Unit
}

@Singleton
class AndroidTempCacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) : TempCacheCleaner {
    override fun clearSensitiveTempFiles() {
        listOf("pdfs", "capture").forEach { dirName ->
            runCatching { File(context.cacheDir, dirName).deleteRecursively() }
        }
    }
}
