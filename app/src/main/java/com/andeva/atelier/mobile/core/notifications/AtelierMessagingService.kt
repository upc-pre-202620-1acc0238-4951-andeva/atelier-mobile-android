package com.andeva.atelier.mobile.core.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.andeva.atelier.mobile.MainActivity
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AtelierMessagingService : FirebaseMessagingService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var deviceTokenRegistrar: DeviceTokenRegistrar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            if (sessionManager.getSession() != null) deviceTokenRegistrar.register(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val destination = PushRouter.route(message.data) ?: return          // tipo desconocido: se ignora
        val notificationManager = NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled()) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, destination.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, NotificationChannels.idFor(PushRouter.channelFor(destination)))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            notificationManager.notify(destination.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revocado entre la comprobación y el envío.
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
