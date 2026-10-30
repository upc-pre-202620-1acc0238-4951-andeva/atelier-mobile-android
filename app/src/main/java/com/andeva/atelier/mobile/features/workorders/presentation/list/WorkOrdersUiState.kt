package com.andeva.atelier.mobile.features.workorders.presentation.list

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder

/** Órdenes no cerradas de la sede, desde Room (se ven sin red). Punto de entrada del supervisor a las tareas (US33). */
data class WorkOrdersUiState(
    val orders: List<WorkOrder> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    /** La última recarga falló (sin red): se sigue con la caché. */
    val showingCache: Boolean = false,
    val error: UiText? = null,
    val canRead: Boolean = true
)
