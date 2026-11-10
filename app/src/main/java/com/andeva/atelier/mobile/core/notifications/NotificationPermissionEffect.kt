package com.andeva.atelier.mobile.core.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Pide `POST_NOTIFICATIONS` (API 33+) una sola vez, cuando ya hay sesión. Si el usuario lo rechaza no pasa nada:
 * la app sigue funcionando y solo dejan de verse los avisos del sistema.
 */
@Composable
fun NotificationPermissionEffect(signedIn: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* sin acción */ }
    var asked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(signedIn) {
        if (!signedIn || asked) return@LaunchedEffect
        asked = true
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
