package com.andeva.atelier.mobile.features.attendance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.attendance.application.ClockInUseCase
import com.andeva.atelier.mobile.features.attendance.application.ClockOutUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveTodayAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveWeeklyScheduleUseCase
import com.andeva.atelier.mobile.features.attendance.application.RefreshAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import com.andeva.atelier.mobile.features.attendance.presentation.toAttendanceUiText
import com.andeva.atelier.mobile.features.diagnostics.application.GetObdDevicesUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.tasks.application.ObserveMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.application.RefreshMyTasksUseCase
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val observeToday: ObserveTodayAttendanceUseCase,
    private val observeSchedule: ObserveWeeklyScheduleUseCase,
    private val refreshAttendance: RefreshAttendanceUseCase,
    private val clockInUseCase: ClockInUseCase,
    private val clockOutUseCase: ClockOutUseCase,
    private val locationProvider: LocationProvider,
    private val observeMyTasks: ObserveMyTasksUseCase,
    private val refreshMyTasks: RefreshMyTasksUseCase,
    private val getObdDevices: GetObdDevicesUseCase,
    observePendingCount: ObservePendingOperationCountUseCase? = null,
    private val pushInbox: PushInbox? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeDashboardUiState())
    val uiState: StateFlow<EmployeeDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sessionManager.session, observeToday(), observeSchedule()) { session, today, schedule ->
                Triple(session, today, schedule)
            }.collect { (session, today, schedule) ->
                _uiState.update {
                    it.copy(
                        session = session,
                        today = today,
                        schedule = schedule
                    )
                }
            }
        }

        viewModelScope.launch {
            observeMyTasks().collect { tasks ->
                val active = tasks.firstOrNull { it.status == TaskStatus.IN_PROGRESS }
                    ?: tasks.firstOrNull { it.status == TaskStatus.ON_HOLD }
                val completed = tasks.count { it.status == TaskStatus.COMPLETED }
                val pending = tasks.count { it.status != TaskStatus.COMPLETED }
                _uiState.update {
                    it.copy(
                        activeTask = active,
                        assignedTasks = tasks,
                        completedTasksCount = completed,
                        pendingTasksCount = pending,
                        totalTasksCount = tasks.size
                    )
                }
            }
        }

        observePendingCount?.let { observePending ->
            viewModelScope.launch {
                observePending().collect { count ->
                    _uiState.update { it.copy(pendingSyncCount = count) }
                }
            }
        }

        pushInbox?.let { inbox ->
            viewModelScope.launch {
                inbox.history.collect { records ->
                    _uiState.update { it.copy(unreadNotificationsCount = records.count { !it.read }) }
                }
            }
        }

        refresh()
    }

    fun previewLocation() {
        viewModelScope.launch {
            locationProvider.currentLocation().onSuccess { location ->
                _uiState.update { it.copy(deviceLocation = location) }
            }
        }
    }

    fun onLocationPermissionDenied() =
        _uiState.update { it.copy(error = UiText.Resource(R.string.attendance_location_permission_required), deviceLocation = null) }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null, notice = null) }
            launch { refreshAttendance() }
            launch { refreshMyTasks() }
            launch {
                getObdDevices(DeviceStatus.ACTIVE).onSuccess { list ->
                    _uiState.update { it.copy(activeObdCount = list.size) }
                }
            }
            previewLocation()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clockIn() {
        val session = _uiState.value.session ?: return
        if (!session.has(Permission.ATTENDANCE_CLOCK_IN)) {
            showError(R.string.attendance_permission_clock_in)
            return
        }
        if (session.branchId.isNullOrBlank() || session.shiftId.isNullOrBlank()) {
            showError(R.string.attendance_missing_assignment)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAttendance = true, error = null, notice = null) }
            clockInUseCase(session.branchId, session.shiftId)
                .onSuccess { result ->
                    val notice = when (result.outcome) {
                        is SubmitOutcome.Recorded -> UiText.Resource(R.string.attendance_clock_in_recorded)
                        SubmitOutcome.Queued, SubmitOutcome.AlreadyQueued -> UiText.Resource(R.string.attendance_clock_in_queued)
                    }
                    _uiState.update { it.copy(isSubmittingAttendance = false, notice = notice) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmittingAttendance = false, error = error.toAttendanceUiText()) }
                }
        }
    }

    fun clockOut() {
        val session = _uiState.value.session ?: return
        if (!session.has(Permission.ATTENDANCE_CLOCK_OUT)) {
            showError(R.string.attendance_permission_clock_out)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAttendance = true, error = null, notice = null) }
            clockOutUseCase()
                .onSuccess { outcome ->
                    val notice = when (outcome) {
                        is SubmitOutcome.Recorded -> UiText.Resource(R.string.attendance_clock_out_recorded)
                        SubmitOutcome.Queued, SubmitOutcome.AlreadyQueued -> UiText.Resource(R.string.attendance_clock_out_queued)
                    }
                    _uiState.update { it.copy(isSubmittingAttendance = false, notice = notice) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmittingAttendance = false, error = error.toAttendanceUiText()) }
                }
        }
    }

    private fun showError(resourceId: Int) =
        _uiState.update { it.copy(error = UiText.Resource(resourceId), notice = null) }
}
