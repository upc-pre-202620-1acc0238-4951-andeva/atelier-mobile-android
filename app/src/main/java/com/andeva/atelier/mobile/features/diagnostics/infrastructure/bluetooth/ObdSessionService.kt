package com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.andeva.atelier.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Foreground service de tipo `connectedDevice` (docs/04-mobile-integration.md §4): mientras dure el muestreo en vivo,
 * Android no corta el socket del ELM327. La conexión vive en `LiveDiagnosticSession`; el servicio solo mantiene el
 * proceso en primer plano. Su canal es de la feature: los de `core/notifications` son para los push.
 */
class ObdSessionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.diagnostics_session_notification_title))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } catch (_: SecurityException) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_diagnostics), NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val CHANNEL_ID = "diagnostics"
        private const val NOTIFICATION_ID = 8001
    }
}

/** Arranca y detiene [ObdSessionService]; interfaz para no depender de Android en la sesión en vivo. */
interface ObdForegroundController {
    fun start()
    fun stop()
}

class ObdSessionServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : ObdForegroundController {

    /**
     * Desde API 34 el tipo `connectedDevice` exige `BLUETOOTH_CONNECT` concedido: sin él no se arranca y la sesión
     * sigue igual (solo pierde la protección en segundo plano).
     */
    override fun start() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        runCatching { ContextCompat.startForegroundService(context, Intent(context, ObdSessionService::class.java)) }
    }

    override fun stop() {
        context.stopService(Intent(context, ObdSessionService::class.java))
    }
}
