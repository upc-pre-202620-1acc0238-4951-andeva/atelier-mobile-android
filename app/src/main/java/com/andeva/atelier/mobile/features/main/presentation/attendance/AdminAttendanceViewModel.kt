package com.andeva.atelier.mobile.features.main.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.application.GetBranchDailyAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.application.ReviewJustificationUseCase
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAttendanceViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getBranchDaily: GetBranchDailyAttendanceUseCase,
    private val reviewJustification: ReviewJustificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAttendanceUiState())
    val uiState: StateFlow<AdminAttendanceUiState> = _uiState.asStateFlow()

    private var lastBranchId: String? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val audit = session?.has(Permission.ATTENDANCE_AUDIT_ALL) == true
                val approve = session?.has(Permission.JUSTIFICATIONS_APPROVE) == true
                val branchChanged = lastBranchId != null && session?.branchId != lastBranchId
                lastBranchId = session?.branchId
                _uiState.update { state ->
                    state.copy(
                        canAuditAll = audit,
                        canApproveJustifications = approve,
                        tab = if (!audit && state.tab != AdminAttendanceTab.MY_ATTENDANCE) {
                            AdminAttendanceTab.MY_ATTENDANCE
                        } else {
                            state.tab
                        }
                    )
                }
                if (audit && (branchChanged || _uiState.value.branchRecords.isEmpty())) {
                    loadBranchDaily()
                } else if (!audit) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onTabChange(tab: AdminAttendanceTab) = _uiState.update { it.copy(tab = tab, error = null, notice = null) }

    fun loadBranchDaily() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getBranchDaily()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, branchRecords = list) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUiText()) }
                }
        }
    }

    fun onReviewJustification(id: AttendanceId, approve: Boolean) {
        if (!_uiState.value.canApproveJustifications || _uiState.value.reviewingId != null) return
        _uiState.update { it.copy(reviewingId = id, error = null, notice = null) }
        viewModelScope.launch {
            reviewJustification(id, approve)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            reviewingId = null,
                            branchRecords = state.branchRecords.map { item ->
                                if (item.attendance.id == id) item.copy(attendance = updated) else item
                            },
                            notice = UiText.Resource(
                                if (approve) R.string.admin_attendance_justification_approved
                                else R.string.admin_attendance_justification_rejected
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(reviewingId = null, error = e.toUiText()) }
                }
        }
    }
}
