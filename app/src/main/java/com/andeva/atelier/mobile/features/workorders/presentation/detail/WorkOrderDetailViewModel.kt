package com.andeva.atelier.mobile.features.workorders.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkOrderDetailUiState(
    val detail: WorkOrderDetail? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null
)

/**
 * Detalle en línea de la orden (`GET /work-orders/{id}`): sus tareas con estado, mecánico y repuestos. Desde aquí el
 * supervisor abre una tarea ajena para corregir o devolver repuestos (US33). Al volver la red recarga sola.
 */
@HiltViewModel
class WorkOrderDetailViewModel @Inject constructor(
    private val getDetail: GetWorkOrderDetailUseCase,
    connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(WorkOrderDetailUiState())
    val uiState: StateFlow<WorkOrderDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                if (online && _uiState.value.detail == null && !_uiState.value.isLoading) load()
            }
        }
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getDetail(workOrderId)
                .onSuccess { detail -> _uiState.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toWorkOrderUiText()) } }
        }
    }
}
