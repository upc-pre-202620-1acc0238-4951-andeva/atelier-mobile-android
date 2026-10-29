package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.tasks.domain.Task

/** US26: bandeja del mecánico. Las tareas salen de Room, así que se ven sin red. */
data class MyTasksUiState(
    val tasks: List<Task> = emptyList(),
    /** Sin lectura todavía de la caché: evita mostrar "no tienes tareas" un instante al abrir. */
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    /** La última recarga falló (sin red): se sigue con la caché. */
    val showingCache: Boolean = false,
    val error: UiText? = null,
    val canRead: Boolean = true
)
