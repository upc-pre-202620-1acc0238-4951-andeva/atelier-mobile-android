package com.andeva.atelier.mobile.features.bays.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.bays.application.CreateBayUseCase
import com.andeva.atelier.mobile.features.bays.application.ObserveBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.PollBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.ReassignBayUseCase
import com.andeva.atelier.mobile.features.bays.application.ReassignTaskMechanicUseCase
import com.andeva.atelier.mobile.features.bays.application.RefreshBayBoardUseCase
import com.andeva.atelier.mobile.features.bays.application.ReleaseBayUseCase
import com.andeva.atelier.mobile.features.bays.application.RestoreBayUseCase
import com.andeva.atelier.mobile.features.bays.application.SetBayMaintenanceUseCase
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.InvalidBayInputException
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetBranchMechanicsUseCase
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
    private val createBayUseCase: CreateBayUseCase,
    connectivity: ConnectivityObserver,
    private val releaseBay: ReleaseBayUseCase? = null,
    private val setBayMaintenance: SetBayMaintenanceUseCase? = null,
    private val restoreBay: RestoreBayUseCase? = null,
    private val pollBayBoard: PollBayBoardUseCase? = null,
    private val getBranchMechanics: GetBranchMechanicsUseCase? = null,
    private val reassignTaskMechanic: ReassignTaskMechanicUseCase? = null
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
                        canReadWorkOrders = session?.has(Permission.WORK_ORDERS_READ) == true,
                        canAssignBay = session?.has(Permission.BAYS_REASSIGN) == true,
                        canReleaseBay = session?.has(Permission.BAYS_REASSIGN) == true,
                        canManageBays = session?.has(Permission.BAYS_MANAGE) == true
                    )
                }
                if (branchChanged) refresh()
            }
        }
        viewModelScope.launch {
            observeBayBoard().collect { items ->
                _uiState.update { state ->
                    val refreshedSelected = state.selectedBayItem?.let { selected ->
                        items.firstOrNull { it.bay.id == selected.bay.id } ?: selected
                    }
                    state.copy(board = items, selectedBayItem = refreshedSelected)
                }
            }
        }
        viewModelScope.launch {
            observeOrders().collect { orders ->
                _uiState.update { it.copy(activeOrders = orders) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            if (_uiState.value.canReadWorkOrders) {
                refreshOrders()
            }
            if (_uiState.value.availableMechanics.isEmpty()) {
                getBranchMechanics?.invoke()?.onSuccess { list ->
                    _uiState.update { it.copy(availableMechanics = list) }
                }
            }
            refreshBayBoard()
                .onSuccess { _uiState.update { it.copy(isRefreshing = false, hasLoaded = true) } }
                .onFailure { e -> _uiState.update { it.copy(isRefreshing = false, hasLoaded = true, error = e.toUiText()) } }
        }
    }

    suspend fun pollWhileVisible() {
        val poll = pollBayBoard ?: return
        poll().collect { result ->
            result.onSuccess {
                _uiState.update { it.copy(hasLoaded = true) }
            }
        }
    }

    fun onOpenReassignTaskMechanic(taskId: String?) = _uiState.update {
        if (it.isUpdatingBay) it else it.copy(reassigningTaskId = taskId, error = null)
    }

    fun onConfirmReassignTaskMechanic(taskId: String, mechanicId: String) {
        val reassign = reassignTaskMechanic ?: return
        val state = _uiState.value
        if (!state.canReleaseBay || state.isUpdatingBay || !state.isOnline) return
        val mechanicName = state.availableMechanics.firstOrNull { it.membershipId == mechanicId }?.displayName.orEmpty()
        _uiState.update { it.copy(isUpdatingBay = true, error = null) }
        viewModelScope.launch {
            reassign(taskId, mechanicId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingBay = false,
                            reassigningTaskId = null,
                            notice = UiText.Resource(R.string.order_mechanic_assigned_notice, mechanicName)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUpdatingBay = false, error = e.toUiText()) }
                }
        }
    }

    fun onFilterChange(status: BayStatus?) = _uiState.update { it.copy(statusFilter = status) }

    fun onOpenBayActions(item: BayBoardItem) {
        _uiState.update {
            it.copy(
                selectedBayItem = item,
                showMaintenanceInput = false,
                maintenanceReason = "",
                invalidField = null,
                error = null,
                notice = null
            )
        }
    }

    fun onDismissBayActions() = _uiState.update {
        if (it.isUpdatingBay || it.isAssigning) it
        else it.copy(
            selectedBayItem = null,
            reassigningTaskId = null,
            showMaintenanceInput = false,
            maintenanceReason = "",
            invalidField = null,
            error = null
        )
    }

    fun onOpenAssignOrder(bay: WorkBay) {
        if (!_uiState.value.canAssignBay || !_uiState.value.isOnline || !bay.canReceive) return
        _uiState.update { it.copy(targetBayForAssign = bay, error = null, notice = null) }
    }

    fun onDismissAssignOrder() = _uiState.update {
        if (it.isAssigning) it else it.copy(targetBayForAssign = null)
    }

    fun onOpenCreateBay() = _uiState.update {
        if (it.canManageBays && it.isOnline) it.copy(showCreateDialog = true, error = null, notice = null) else it
    }

    fun onDismissCreateBay() = _uiState.update {
        if (it.isCreatingBay) it else it.copy(showCreateDialog = false)
    }

    fun onCreateBay(name: String, bayType: BayType) {
        if (name.isBlank() || _uiState.value.isCreatingBay || !_uiState.value.isOnline) return
        _uiState.update { it.copy(isCreatingBay = true, error = null) }
        viewModelScope.launch {
            createBayUseCase(name.trim(), bayType)
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
        val bay = _uiState.value.targetBayForAssign ?: _uiState.value.selectedBayItem?.bay ?: return
        if (_uiState.value.isAssigning || !_uiState.value.isOnline) return
        _uiState.update { it.copy(isAssigning = true, error = null) }
        viewModelScope.launch {
            reassignBay(workOrderId, bay)
                .onSuccess {
                    if (_uiState.value.canReadWorkOrders) refreshOrders()
                    refreshBayBoard()
                    _uiState.update {
                        it.copy(
                            isAssigning = false,
                            targetBayForAssign = null,
                            selectedBayItem = null,
                            notice = UiText.Resource(R.string.order_bay_assigned_notice, bay.name)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isAssigning = false, error = e.toUiText()) }
                }
        }
    }

    fun onReassignSelectedBayOrder(targetBay: WorkBay) {
        val occupant = _uiState.value.selectedBayItem?.occupant ?: return
        if (!_uiState.value.canAssignBay || _uiState.value.isUpdatingBay || !_uiState.value.isOnline) return
        _uiState.update { it.copy(isUpdatingBay = true, error = null) }
        viewModelScope.launch {
            reassignBay(occupant.workOrderId, targetBay)
                .onSuccess {
                    if (_uiState.value.canReadWorkOrders) refreshOrders()
                    refreshBayBoard()
                    _uiState.update {
                        it.copy(
                            isUpdatingBay = false,
                            selectedBayItem = null,
                            notice = UiText.Resource(R.string.order_bay_assigned_notice, targetBay.name)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUpdatingBay = false, error = e.toUiText()) }
                }
        }
    }

    fun onReleaseSelectedBay() {
        val release = releaseBay ?: return
        val item = _uiState.value.selectedBayItem ?: return
        val occupant = item.occupant ?: return
        if (!_uiState.value.canReleaseBay || _uiState.value.isUpdatingBay || !_uiState.value.isOnline) return
        _uiState.update { it.copy(isUpdatingBay = true, error = null) }
        viewModelScope.launch {
            release(occupant.workOrderId)
                .onSuccess {
                    if (_uiState.value.canReadWorkOrders) refreshOrders()
                    _uiState.update {
                        it.copy(
                            isUpdatingBay = false,
                            selectedBayItem = null,
                            notice = UiText.Resource(R.string.bays_released_notice, item.bay.name)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUpdatingBay = false, error = e.toUiText()) }
                }
        }
    }

    fun onToggleMaintenanceInput(show: Boolean) = _uiState.update {
        if (it.isUpdatingBay) it else it.copy(showMaintenanceInput = show, invalidField = null, error = null)
    }

    fun onMaintenanceReasonChange(reason: String) = _uiState.update {
        if (it.isUpdatingBay) it else it.copy(maintenanceReason = reason, invalidField = null, error = null)
    }

    fun onConfirmSetMaintenance() {
        val setMaintenance = setBayMaintenance ?: return
        val state = _uiState.value
        val bay = state.selectedBayItem?.bay ?: return
        if (!state.canManageBays || state.isUpdatingBay || !state.isOnline) return
        _uiState.update { it.copy(isUpdatingBay = true, invalidField = null, error = null) }
        viewModelScope.launch {
            setMaintenance(bay.id, state.maintenanceReason)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingBay = false,
                            selectedBayItem = null,
                            showMaintenanceInput = false,
                            maintenanceReason = "",
                            notice = UiText.Resource(R.string.bays_maintenance_notice, bay.name)
                        )
                    }
                }
                .onFailure { e ->
                    val invalid = (e as? InvalidBayInputException)?.field
                    val errorMsg = if (invalid == "reason") {
                        UiText.Resource(R.string.bays_maintenance_field_error)
                    } else {
                        e.toUiText()
                    }
                    _uiState.update {
                        it.copy(isUpdatingBay = false, invalidField = invalid, error = errorMsg)
                    }
                }
        }
    }

    fun onRestoreSelectedBay() {
        val restore = restoreBay ?: return
        val state = _uiState.value
        val bay = state.selectedBayItem?.bay ?: return
        if (!state.canManageBays || state.isUpdatingBay || !state.isOnline) return
        _uiState.update { it.copy(isUpdatingBay = true, error = null) }
        viewModelScope.launch {
            restore(bay.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingBay = false,
                            selectedBayItem = null,
                            notice = UiText.Resource(R.string.bays_restored_notice, bay.name)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUpdatingBay = false, error = e.toUiText()) }
                }
        }
    }
}
