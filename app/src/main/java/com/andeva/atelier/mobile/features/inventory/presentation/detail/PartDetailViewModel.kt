package com.andeva.atelier.mobile.features.inventory.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.ObservePartBatchesUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartBatchesUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * US32 E1: ficha del repuesto y sus lotes cronológicos. La ficha y los lotes salen de Room (se ven sin red);
 * los lotes se recargan con `GET /inventory/items/{id}/batches` solo con `inventory:batches:read`.
 */
@HiltViewModel
class PartDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observePart: ObservePartUseCase,
    observeBatches: ObservePartBatchesUseCase,
    private val refreshBatches: RefreshPartBatchesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val partId: String = savedStateHandle["partId"] ?: ""

    private val _uiState = MutableStateFlow(PartDetailUiState())
    val uiState: StateFlow<PartDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canReadBatches = session?.has(Permission.BATCHES_READ) == true) }
            }
        }
        viewModelScope.launch {
            observePart(partId).collect { part -> _uiState.update { it.copy(part = part) } }
        }
        viewModelScope.launch {
            observeBatches(partId).collect { batches ->
                // Orden FIFO defensivo: del más antiguo al más nuevo; sin fecha al final.
                val fifo = batches.sortedBy { it.arrivalDate ?: Instant.MAX }
                _uiState.update { it.copy(batches = fifo) }
            }
        }
        refresh()
    }

    /** Recarga los lotes; sin red se siguen mostrando los guardados. */
    fun refresh(force: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            refreshBatches(partId, force)
                .onSuccess { _uiState.update { it.copy(isLoading = false, showingCache = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, showingCache = true, error = e.toUiText()) }
                }
        }
    }
}
