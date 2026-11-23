package com.andeva.atelier.mobile.features.workorders.presentation.list

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.time.Instant

/** Órdenes no cerradas de la sede, desde Room (se ven sin red). Punto de entrada del supervisor a las tareas (US33). */
data class WorkOrdersUiState(
    val orders: List<WorkOrder> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    /** La última recarga falló (sin red): se sigue con la caché. */
    val showingCache: Boolean = false,
    val lastRefreshedAt: Instant? = null,
    val error: UiText? = null,
    val canRead: Boolean = true,
    val canCreate: Boolean = false,
    /** Filtro por estado; `null` muestra todas. */
    val statusFilter: WorkOrderStatus? = null,
    /** Texto de la búsqueda por placa. */
    val query: String = "",
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0
) {
    /** Las órdenes que pasan el filtro de estado y la búsqueda por placa (sin guion ni mayúsculas). */
    val visibleOrders: List<WorkOrder>
        get() {
            val needle = query.filter { it.isLetterOrDigit() }.uppercase()
            return orders.filter { order ->
                (statusFilter == null || order.status == statusFilter) &&
                    (needle.isEmpty() || order.plate.orEmpty().filter { it.isLetterOrDigit() }.uppercase().contains(needle))
            }
        }

    /** Sin caché que pintar: se muestra el esqueleto. */
    val showSkeleton: Boolean get() = !isLoaded || (orders.isEmpty() && isRefreshing && lastRefreshedAt == null && error == null)
}
