package com.andeva.atelier.mobile.core.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WorkshopCacheEvent {
    /** Una tarea o estado de orden cambió en el backend (p. ej. cierre de tarea US30). */
    data class WorkOrderMutated(val workOrderId: String) : WorkshopCacheEvent

    /** Un vehículo fue entregado en patio y liberó su bahía (US39). */
    data class VehicleDelivered(
        val workOrderId: String? = null,
        val branchId: String? = null
    ) : WorkshopCacheEvent
}

/**
 * Bus de invalidación de cachés Room con ámbito de aplicación. Desacopla features hermanos (tasks → workorders,
 * cashier → bays) y garantiza que los refrescos corran en `appScope` sin cancelarse si el usuario navega atrás.
 */
@Singleton
class WorkshopCacheEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<WorkshopCacheEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<WorkshopCacheEvent> = _events.asSharedFlow()

    fun emit(event: WorkshopCacheEvent) {
        _events.tryEmit(event)
    }

    fun tryEmit(event: WorkshopCacheEvent) {
        _events.tryEmit(event)
    }
}
