package com.andeva.atelier.mobile.features.workorders.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Toma una foto con la cámara del sistema (US13, US14). Devuelve la función que abre la cámara: pide `CAMERA` si
 * falta (el manifiesto la declara, así que sin ella la cámara del sistema no se puede abrir) y entrega el archivo a
 * [onCaptured]; si el permiso se niega llama a [onDenied]. El archivo queda en `cacheDir/capture`: quien lo recibe
 * lo comprime y lo encola (`PhotoCapture`), y la ruta se conserva si Android recrea la pantalla mientras se toma.
 */
@Composable
fun rememberPhotoCapture(onCaptured: (File) -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnDenied by rememberUpdatedState(onDenied)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPath
        pendingPath = null
        if (path != null) {
            val file = File(path)
            if (saved) currentOnCaptured(file) else file.delete()
        }
    }

    fun openCamera() {
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        val file = File.createTempFile("photo_", ".jpg", dir)
        pendingPath = file.path
        takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else currentOnDenied()
    }

    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) openCamera() else requestPermission.launch(Manifest.permission.CAMERA)
    }
}
