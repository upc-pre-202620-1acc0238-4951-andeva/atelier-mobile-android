package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus

/** Filtros de la bandeja del mecánico en bahía. */
enum class TaskFilter { ALL, IN_PROGRESS, PAUSED, ASSIGNED, SUSPENDED }

/** US26: bandeja del mecánico. Las tareas salen de Room, así que se ven sin red. */
data class MyTasksUiState(
    val tasks: List<Task> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    /** Sin lectura todavía de la caché: evita mostrar "no tienes tareas" un instante al abrir. */
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    /** La última recarga falló (sin red): se sigue con la caché. */
    val showingCache: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = true,
    val isWorkshopScope: Boolean = false,
    val tenantName: String = "",
    val pendingSyncCount: Int = 0,
    val unreadNotificationsCount: Int = 0
) {
    val showSkeleton: Boolean
        get() = canRead && (!isLoaded || (isRefreshing && tasks.isEmpty() && error == null))

    val pendingCount: Int
        get() = tasks.count { it.status == TaskStatus.ASSIGNED || it.status == TaskStatus.PENDING }

    val inProgressCount: Int
        get() = tasks.count { it.status == TaskStatus.IN_PROGRESS }

    val pausedCount: Int
        get() = tasks.count { it.status == TaskStatus.ON_HOLD }

    val visibleTasks: List<Task>
        get() = when (selectedFilter) {
            TaskFilter.ALL -> tasks
            TaskFilter.IN_PROGRESS -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            TaskFilter.PAUSED -> tasks.filter { it.isPausedByMechanic }
            TaskFilter.ASSIGNED -> tasks.filter { it.status == TaskStatus.ASSIGNED || it.status == TaskStatus.PENDING }
            TaskFilter.SUSPENDED -> tasks.filter { it.isSuspended || it.hasPendingSuspension }
        }
}
