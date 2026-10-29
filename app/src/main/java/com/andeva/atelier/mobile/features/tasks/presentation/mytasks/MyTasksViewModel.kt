package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.application.RefreshMyTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * US26: Mis tareas. Observa la caché Room (en curso → en pausa → asignadas) y la recarga con `GET /tasks/my`
 * al abrir y al deslizar. Si la recarga falla se sigue con la caché.
 */
@HiltViewModel
class MyTasksViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeMyTasks: ObserveMyTasksUseCase,
    private val refreshMyTasks: RefreshMyTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTasksUiState())
    val uiState: StateFlow<MyTasksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canRead = session?.has(Permission.TASKS_READ) == true) }
            }
        }
        viewModelScope.launch {
            observeMyTasks().collect { tasks -> _uiState.update { it.copy(tasks = tasks, isLoaded = true) } }
        }
        refresh()
    }

    /** Al abrir, al deslizar y cuando llega el push `TASK_ASSIGNED`. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            refreshMyTasks()
                .onSuccess { _uiState.update { it.copy(isRefreshing = false, showingCache = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isRefreshing = false, showingCache = true, error = e.toUiText()) }
                }
        }
    }
}
