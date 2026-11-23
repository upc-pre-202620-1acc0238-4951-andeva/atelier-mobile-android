package com.andeva.atelier.mobile.features.main.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.application.GetBranchDailyAttendanceUseCase
import com.andeva.atelier.mobile.features.auth.application.GetSelectableBranchesUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.team.application.ApproveJustificationUseCase
import com.andeva.atelier.mobile.features.team.application.GetPendingJustificationsUseCase
import com.andeva.atelier.mobile.features.team.application.RejectJustificationUseCase
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Cuánto se queda a la vista el resultado de una decisión antes de retirarse de la lista. */
private const val RESOLVED_LINGER_MS = 1_600L

@HiltViewModel
class AdminAttendanceViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getBranches: GetSelectableBranchesUseCase,
    private val getBranchDaily: GetBranchDailyAttendanceUseCase,
    private val getPendingJustifications: GetPendingJustificationsUseCase,
    private val approveJustification: ApproveJustificationUseCase,
    private val rejectJustification: RejectJustificationUseCase,
    connectivity: ConnectivityObserver,
    clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAttendanceUiState(today = BusinessTime.limaDate(clock.instant())))
    val uiState: StateFlow<AdminAttendanceUiState> = _uiState.asStateFlow()

    private var lastBranchId: String? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val branchChanged = lastBranchId != null && session?.branchId != lastBranchId
                lastBranchId = session?.branchId
                val audit = session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true
                _uiState.update { state ->
                    state.copy(
                        canAuditAll = audit,
                        canClockIn = session?.has(Permission.ATTENDANCE_CLOCK_IN) == true,
                        canApproveJustifications = session?.has(Permission.JUSTIFICATIONS_APPROVE) == true,
                        // Sin permiso de auditar, lo único que queda es la asistencia propia.
                        tab = if (!audit) TeamTab.MINE else state.tab
                    )
                }
                if (branchChanged) refresh()
            }
        }
        viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } } }
        refresh()
    }

    fun onTabChange(tab: TeamTab) = _uiState.update { it.copy(tab = tab) }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            val session = observeSession().firstOrNull()
            if (session?.has(Permission.ATTENDANCE_AUDIT_ALL) != true) {
                _uiState.update { it.copy(isRefreshing = false, hasLoaded = true) }
                return@launch
            }
            var failure: Throwable? = null
            if (session.has(Permission.BRANCHES_READ)) {
                getBranches().onSuccess { branches ->
                    _uiState.update { state -> state.copy(branchName = branches.firstOrNull { it.id == session.branchId }?.name) }
                }
            }
            getBranchDaily()
                .onSuccess { list -> _uiState.update { it.copy(records = list) } }
                .onFailure { failure = it }
            getPendingJustifications().onSuccess { pending ->
                _uiState.update {
                    it.copy(
                        justifications = pending.items.filter { item -> item.awaitsReview },
                        failedJustificationDays = pending.failedDates.size,
                        resolved = emptyMap()
                    )
                }
            }
            _uiState.update { it.copy(isRefreshing = false, hasLoaded = true, error = failure?.toUiText()) }
        }
    }

    fun onOpenJustification(item: TeamAttendance) {
        if (item.id in _uiState.value.resolved) return
        _uiState.update { it.copy(reviewing = item, reviewError = null) }
    }

    fun onDismissJustification() {
        if (_uiState.value.isSubmittingReview) return
        _uiState.update { it.copy(reviewing = null, reviewError = null) }
    }

    fun onReviewJustification(approve: Boolean) {
        val state = _uiState.value
        val item = state.reviewing ?: return
        if (!state.canApproveJustifications || state.isSubmittingReview || !state.isOnline) return
        _uiState.update { it.copy(isSubmittingReview = true, reviewError = null) }
        viewModelScope.launch {
            val result = if (approve) approveJustification(item.id) else rejectJustification(item.id)
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isSubmittingReview = false, reviewing = null, resolved = it.resolved + (item.id to approve))
                    }
                    delay(RESOLVED_LINGER_MS)
                    _uiState.update { s ->
                        s.copy(justifications = s.justifications.filterNot { it.id == item.id }, resolved = s.resolved - item.id)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isSubmittingReview = false, reviewError = e.toUiText()) } }
        }
    }
}
