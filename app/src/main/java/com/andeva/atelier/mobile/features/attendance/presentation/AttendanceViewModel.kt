package com.andeva.atelier.mobile.features.attendance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.application.ClockInUseCase
import com.andeva.atelier.mobile.features.attendance.application.ClockOutUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveAttendanceHistoryUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveTodayAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.application.ObserveWeeklyScheduleUseCase
import com.andeva.atelier.mobile.features.attendance.application.RefreshAttendanceUseCase
import com.andeva.atelier.mobile.features.attendance.application.SubmitJustificationUseCase
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.InvalidJustificationException
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceValidation
import com.andeva.atelier.mobile.features.attendance.domain.LocationPermissionDeniedException
import com.andeva.atelier.mobile.features.attendance.domain.LocationTooInaccurateException
import com.andeva.atelier.mobile.features.attendance.domain.LocationUnavailableException
import com.andeva.atelier.mobile.features.attendance.domain.MissingAssignmentException
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    sessionManager: SessionManager,
    observeToday: ObserveTodayAttendanceUseCase,
    observeHistory: ObserveAttendanceHistoryUseCase,
    observeSchedule: ObserveWeeklyScheduleUseCase,
    private val refreshAttendance: RefreshAttendanceUseCase,
    private val clockInUseCase: ClockInUseCase,
    private val clockOutUseCase: ClockOutUseCase,
    private val submitJustificationUseCase: SubmitJustificationUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sessionManager.session, observeToday(), observeSchedule(), observeHistory()) { session, today, schedule, history ->
                Snapshot(session, today, schedule, history)
            }.collect { snapshot ->
                _uiState.update {
                    it.copy(
                        session = snapshot.session,
                        today = snapshot.today,
                        schedule = snapshot.schedule,
                        history = snapshot.history
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null, notice = null) }
            refreshAttendance().onFailure { error ->
                _uiState.update { it.copy(error = error.toAttendanceUiText()) }
            }
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
            _uiState.update { it.copy(isSubmitting = true, error = null, notice = null) }
            clockInUseCase(session.branchId, session.shiftId)
                .onSuccess { result ->
                    val notice = when (result.outcome) {
                        is SubmitOutcome.Recorded -> UiText.Resource(R.string.attendance_clock_in_recorded)
                        SubmitOutcome.Queued -> UiText.Resource(R.string.attendance_clock_in_queued)
                        SubmitOutcome.AlreadyQueued -> UiText.Resource(R.string.attendance_already_queued)
                    }
                    val geofenceWarning = result.geofence?.let { geofence ->
                        when {
                            geofence.isMock -> UiText.Resource(R.string.attendance_mock_location_warning)
                            !geofence.isInside -> UiText.Resource(R.string.attendance_outside_geofence_warning, geofence.distanceMeters.toInt())
                            else -> null
                        }
                    }
                    _uiState.update { it.copy(notice = notice, locationWarning = geofenceWarning) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toAttendanceUiText()) } }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    /**
     * Obtiene una ubicación fresca para la vista previa de la geocerca. La marcación vuelve a
     * solicitar ubicación dentro de ClockInUseCase; esta vista previa nunca autoriza la marcación.
     */
    fun previewLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, error = null, locationWarning = null) }
            locationProvider.currentLocation()
                .onSuccess { location ->
                    val warning = when {
                        location.isMock -> UiText.Resource(R.string.attendance_mock_location_warning)
                        !AttendanceValidation.isAccurate(location) ->
                            UiText.Resource(R.string.attendance_location_inaccurate, location.accuracyMeters.toInt())
                        else -> null
                    }
                    _uiState.update { it.copy(deviceLocation = location, locationWarning = warning) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.toAttendanceUiText(), deviceLocation = null)
                    }
                }
            _uiState.update { it.copy(isLocating = false) }
        }
    }

    fun onLocationPermissionDenied() = showError(R.string.attendance_location_permission_required)

    fun clockOut() {
        val session = _uiState.value.session ?: return
        if (!session.has(Permission.ATTENDANCE_CLOCK_OUT)) {
            showError(R.string.attendance_permission_clock_out)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, notice = null) }
            clockOutUseCase()
                .onSuccess { outcome ->
                    val notice = when (outcome) {
                        is SubmitOutcome.Recorded -> UiText.Resource(R.string.attendance_clock_out_recorded)
                        SubmitOutcome.Queued -> UiText.Resource(R.string.attendance_clock_out_queued)
                        SubmitOutcome.AlreadyQueued -> UiText.Resource(R.string.attendance_already_queued)
                    }
                    _uiState.update { it.copy(notice = notice) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toAttendanceUiText()) } }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun requestJustification(attendance: Attendance) {
        val session = _uiState.value.session ?: return
        if (!session.has(Permission.JUSTIFICATIONS_CREATE)) {
            showError(R.string.attendance_permission_justification)
            return
        }
        _uiState.update { it.copy(justificationFor = attendance, justification = "", error = null, notice = null) }
    }

    fun onJustificationChange(value: String) {
        _uiState.update { it.copy(justification = value.take(500), error = null) }
    }

    fun cancelJustification() {
        _uiState.update { it.copy(justificationFor = null, justification = "") }
    }

    fun submitJustification() {
        val state = _uiState.value
        val attendance = state.justificationFor ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, notice = null) }
            submitJustificationUseCase(AttendanceId(attendance.id.value), state.justification)
                .onSuccess { outcome ->
                    val notice = when (outcome) {
                        is SubmitOutcome.Recorded -> UiText.Resource(R.string.attendance_justification_recorded)
                        SubmitOutcome.Queued -> UiText.Resource(R.string.attendance_justification_queued)
                        SubmitOutcome.AlreadyQueued -> UiText.Resource(R.string.attendance_already_queued)
                    }
                    _uiState.update { it.copy(notice = notice, justificationFor = null, justification = "") }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toAttendanceUiText()) } }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    private fun showError(resourceId: Int) = _uiState.update { it.copy(error = UiText.Resource(resourceId), notice = null) }

    private fun Throwable.toAttendanceUiText(): UiText = when (this) {
        is LocationPermissionDeniedException -> UiText.Resource(R.string.attendance_location_permission_required)
        is LocationUnavailableException -> UiText.Resource(R.string.attendance_location_unavailable)
        is LocationTooInaccurateException -> UiText.Resource(R.string.attendance_location_inaccurate, accuracyMeters.toInt())
        is MissingAssignmentException -> UiText.Resource(R.string.attendance_missing_assignment)
        is InvalidJustificationException -> UiText.Resource(R.string.attendance_justification_invalid)
        else -> toUiText()
    }

    private data class Snapshot(
        val session: Session?,
        val today: TodayAttendance,
        val schedule: com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule?,
        val history: List<Attendance>
    )
}
