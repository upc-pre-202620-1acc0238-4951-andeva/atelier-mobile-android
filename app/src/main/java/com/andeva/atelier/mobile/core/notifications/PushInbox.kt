package com.andeva.atelier.mobile.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val destination: PushDestination?,
    val channel: PushChannel,
    val receivedAt: Instant = Instant.now(),
    val read: Boolean = false
)

/**
 * Push tocado por el usuario, pendiente de abrir, e historial en memoria de avisos recibidos en la sesión.
 * MainActivity lo publica y la navegación de cada feature lo consume. El push es solo un aviso: la pantalla recarga desde la API.
 */
@Singleton
class PushInbox @Inject constructor() {
    private val _pending = MutableStateFlow<PushDestination?>(null)
    val pending: StateFlow<PushDestination?> = _pending.asStateFlow()

    private val _history = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val history: StateFlow<List<NotificationRecord>> = _history.asStateFlow()

    fun post(destination: PushDestination) {
        _pending.value = destination
    }

    fun record(title: String?, body: String?, destination: PushDestination) {
        val item = NotificationRecord(
            title = title?.takeIf { it.isNotBlank() } ?: defaultTitleFor(destination),
            body = body?.takeIf { it.isNotBlank() } ?: "",
            destination = destination,
            channel = PushRouter.channelFor(destination)
        )
        _history.update { listOf(item) + it.take(49) }
    }

    fun markRead(id: String) {
        _history.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
    }

    fun markAllRead() {
        _history.update { list -> list.map { it.copy(read = true) } }
    }

    fun consume(): PushDestination? = _pending.value.also { _pending.value = null }

    private fun defaultTitleFor(destination: PushDestination): String = when (destination) {
        is PushDestination.TaskDetail -> "Actualización de tarea"
        is PushDestination.TaskSuspensionReview -> "Pausa de tarea por revisar"
        is PushDestination.TaskProducts -> "Repuesto imputado a tarea"
        is PushDestination.WorkOrderDetail -> "Orden de trabajo actualizada"
        is PushDestination.LowStockAlerts -> "Alerta de stock mínimo"
        is PushDestination.CollaboratorAttendance -> "Novedad de asistencia en sede"
        is PushDestination.MyAttendance -> "Justificación revisada"
        is PushDestination.ClockIn -> "Recordatorio de turno"
    }
}
