package com.andeva.atelier.mobile.features.bays.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.bays.application.CreateBayUseCase
import com.andeva.atelier.mobile.features.bays.application.ObserveBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.ReassignBayUseCase
import com.andeva.atelier.mobile.features.bays.application.RefreshBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveBranchWorkOrdersUseCase
import com.andeva.atelier.mobile.features.workorders.application.RefreshBranchWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BaysViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeBayBoard: ObserveBayBoardUseCase,
    observeOrders: ObserveBranchWorkOrdersUseCase,
    private val refreshBayBoard: RefreshBayBoardUseCase,
    private val refreshOrders: RefreshBranchWorkOrdersUseCase,
    private val reassignBay: ReassignBayUseCase,
    private val createBayUseCase: CreateBayUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BaysUiState())
    val uiState: StateFlow<BaysUiState> = _uiState.asStateFlow()

    private var lastBranchId: String? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val branchChanged = lastBranchId != null && session?.branchId != lastBranchId
                lastBranchId = session?.branchId
                _uiState.update {
                    it.copy(
                        canReadBays = session?.has(Permission.BAYS_READ) == true,
                        canAssignBay = session?.has(Permission.BAYS_ASSIGN) == true &&
                            session.has(Permission.WORK_ORDERS_UPDATE),
                        canManageBays = session?.has(Permission.BAYS_MANAGE) == true
                    )
                }
                if (branchChanged) refresh()
            }
        }
        viewModelScope.launch {
            observeBayBoard().collect { items ->
                _uiState.update { it.copy(board = items) }
            }
        }
        viewModelScope.launch {
            observeOrders().collect { orders ->
                _uiState.update { it.copy(activeOrders = orders) }
            }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            refreshOrders()
            refreshBayBoard()
                .onSuccess { _uiState.update { it.copy(isRefreshing = false) } }
                .onFailure { e -> _uiState.update { it.copy(isRefreshing = false, error = e.toUiText()) } }
        }
    }

    fun onFilterChange(status: BayStatus?) = _uiState.update { it.copy(statusFilter = status) }

    fun onOpenAssignOrder(bay: WorkBay) {
        if (!_uiState.value.canAssignBay || !bay.canReceive) return
        _uiState.update { it.copy(targetBayForAssign = bay, error = null, notice = null) }
    }

    fun onDismissAssignOrder() = _uiState.update {
        if (it.isAssigning) it else it.copy(targetBayForAssign = null)
    }

    fun onOpenCreateBay() = _uiState.update {
        if (it.canManageBays) it.copy(showCreateDialog = true, error = null, notice = null) else it
    }

    fun onDismissCreateBay() = _uiState.update {
        if (it.isCreatingBay) it else it.copy(showCreateDialog = false)
    }

    fun onCreateBay(name: String, bayType: BayType) {
        val useCase = createBayUseCase ?: return
        if (name.isBlank() || _uiState.value.isCreatingBay) return
        _uiState.update { it.copy(isCreatingBay = true, error = null) }
        viewModelScope.launch {
            useCase(name.trim(), bayType)
                .onSuccess { created ->
                    refreshBayBoard()
                    _uiState.update {
                        it.copy(
                            isCreatingBay = false,
                            showCreateDialog = false,
                            notice = UiText.Resource(R.string.bays_created_notice, created.name)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCreatingBay = false, error = e.toUiText()) }
                }
        }
    }

    fun onAssignOrderToBay(workOrderId: String) {
        val bay = _uiState.value.targetBayForAssign ?: return
        if (_uiState.value.isAssigning) return
        _uiState.update { it.copy(isAssigning = true, error = null) }
        viewModelScope.launch {
            reassignBay(workOrderId, bay)
                .onSuccess {
                    refreshOrders()
                    refreshBayBoard()
                    _uiState.update {
                        it.copy(
                            isAssigning = false,
                            targetBayForAssign = null,
                            notice = UiText.Resource(R.string.order_bay_assigned_notice, bay.name)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isAssigning = false, error = e.toUiText()) }
                }
        }
    }
}
