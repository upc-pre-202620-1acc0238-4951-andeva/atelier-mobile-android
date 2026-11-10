package com.andeva.atelier.mobile.core.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * Pide `POST_NOTIFICATIONS` (API 33+) una sola vez por inicio de sesión. Si el usuario lo rechaza no pasa nada:
 * la app sigue funcionando y solo dejan de verse los avisos del sistema. Que ya se pidió se guarda en el DataStore
 * de la sesión (ver [NotificationPermissionViewModel]), así que no vuelve a salir en cada arranque.
 */
@Composable
fun NotificationPermissionEffect(
    signedIn: Boolean,
    viewModel: NotificationPermissionViewModel = hiltViewModel()
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* sin acción */ }

    LaunchedEffect(signedIn) {
        if (!signedIn || viewModel.wasAsked()) return@LaunchedEffect
        viewModel.markAsked()
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
