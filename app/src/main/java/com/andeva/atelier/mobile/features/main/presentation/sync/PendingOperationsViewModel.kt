package com.andeva.atelier.mobile.features.main.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.main.application.DiscardPendingOperationUseCase
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationsUseCase
import com.andeva.atelier.mobile.features.main.application.RetryPendingOperationUseCase
import com.andeva.atelier.mobile.features.main.domain.PendingOperationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingOperationsViewModel @Inject constructor(
    observeOperations: ObservePendingOperationsUseCase,
    private val retryOperation: RetryPendingOperationUseCase,
    private val discardOperation: DiscardPendingOperationUseCase,
    private val syncScheduler: SyncScheduler,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingOperationsUiState())
    val uiState: StateFlow<PendingOperationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeOperations().collect { ops ->
                _uiState.update { it.copy(operations = ops, isSyncingNow = false) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun onSyncNow() {
        _uiState.update {
            it.copy(
                isSyncingNow = true,
                notice = UiText.Resource(R.string.sync_requested_notice)
            )
        }
        viewModelScope.launch {
            val failed = _uiState.value.operations.filter { it.status == PendingOperationStatus.FAILED }
            failed.forEach { retryOperation(it.id) }
            syncScheduler.requestSync()
        }
    }

    fun onRetry(id: String) {
        viewModelScope.launch {
            retryOperation(id)
            syncScheduler.requestSync()
        }
    }

    fun onDiscard(id: String) {
        viewModelScope.launch {
            discardOperation(id)
        }
    }
}
