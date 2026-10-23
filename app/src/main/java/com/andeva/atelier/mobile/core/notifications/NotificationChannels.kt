package com.andeva.atelier.mobile.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.andeva.atelier.mobile.R

/** Un canal por grupo: Tareas, Asistencia e Inventario (docs/04-mobile-integration.md §2). */
object NotificationChannels {
    fun idFor(channel: PushChannel) = when (channel) {
        PushChannel.TASKS -> "tasks"
        PushChannel.ATTENDANCE -> "attendance"
        PushChannel.INVENTORY -> "inventory"
    }

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(idFor(PushChannel.TASKS), context.getString(R.string.notification_channel_tasks), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(idFor(PushChannel.ATTENDANCE), context.getString(R.string.notification_channel_attendance), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(idFor(PushChannel.INVENTORY), context.getString(R.string.notification_channel_inventory), NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannels(channels)
    }
}
