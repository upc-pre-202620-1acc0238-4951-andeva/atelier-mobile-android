package com.andeva.atelier.mobile.features.attendance.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LimaZone = ZoneId.of("America/Lima")
private val DateFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-PE"))

private val TimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.clockIn()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(stringResource(R.string.attendance_title), style = MaterialTheme.typography.headlineMedium)
                    Text(state.session?.fullName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(state.session?.tenantName.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.error != null) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(state.error!!.asString(), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            if (state.notice != null) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(state.notice!!.asString(), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            if (state.locationWarning != null) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(state.locationWarning!!.asString(), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.attendance_today), style = MaterialTheme.typography.titleLarge)
                        when (val today = state.today) {
                            TodayAttendance.NotClockedIn -> Text(stringResource(R.string.attendance_not_clocked_in))
                            is TodayAttendance.ClockInQueued -> {
                                Text(stringResource(R.string.attendance_clock_in_pending))
                                Text(stringResource(R.string.attendance_captured_at, formatInstant(today.capturedAt)))
                            }
                            is TodayAttendance.ClockedIn -> {
                                Text(stringResource(R.string.attendance_clocked_in_at, formatInstant(today.attendance.clockIn)))
                                Text(stringResource(R.string.attendance_distance, today.attendance.distanceToBranchMeters.toInt()))
                            }
                            is TodayAttendance.ClockOutQueued -> {
                                Text(stringResource(R.string.attendance_clock_out_pending))
                                Text(stringResource(R.string.attendance_captured_at, formatInstant(today.capturedAt)))
                            }
                            is TodayAttendance.Completed -> {
                                Text(stringResource(R.string.attendance_completed_at, formatInstant(today.attendance.clockOut ?: today.attendance.clockIn)))
                                Text(stringResource(R.string.attendance_worked_minutes, today.attendance.workedMinutes ?: 0L))
                            }
                        }
                        if (state.session?.has(Permission.ATTENDANCE_CLOCK_IN) == true) {
                            AtelierButton(
                                text = stringResource(R.string.attendance_clock_in),
                                onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fineGranted) viewModel.clockIn() else locationLauncher.launch(locationPermissions)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.canClockIn,
                                loading = state.isSubmitting
                            )
                        }
                        if (state.session?.has(Permission.ATTENDANCE_CLOCK_OUT) == true) {
                            AtelierButton(
                                text = stringResource(R.string.attendance_clock_out),
                                onClick = viewModel::clockOut,
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.Tonal,
                                enabled = state.canClockOut,
                                loading = state.isSubmitting
                            )
                        }
                        if (state.isRefreshing) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.attendance_refreshing))
                        }
                        AtelierButton(text = stringResource(R.string.attendance_refresh), onClick = viewModel::refresh, style = AtelierButtonStyle.Text, enabled = !state.isRefreshing)
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.attendance_my_shift), style = MaterialTheme.typography.titleLarge)
                        val schedule = state.schedule
                        if (schedule == null) {
                            Text(stringResource(R.string.attendance_no_shift))
                        } else {
                            Text(schedule.shift.name, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.attendance_shift_hours, schedule.shift.startTime.format(TimeFormatter), schedule.shift.endTime.format(TimeFormatter)))
                            Text(stringResource(R.string.attendance_grace_period, schedule.shift.gracePeriodMinutes))
                            Text(schedule.branch?.name ?: stringResource(R.string.attendance_branch_unavailable))
                            HorizontalDivider()
                            schedule.days.forEach { day ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(dayName(day.dayOfWeek))
                                    Text(if (day.isWorkDay) stringResource(R.string.attendance_work_day) else stringResource(R.string.attendance_rest_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.attendance_history), style = MaterialTheme.typography.titleLarge) }
            if (state.history.isEmpty()) item { Text(stringResource(R.string.attendance_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.history, key = { it.id.value }) { attendance ->
                AttendanceHistoryCard(
                    attendance = attendance,
                    canJustify = state.session?.has(Permission.JUSTIFICATIONS_CREATE) == true,
                    onJustify = { viewModel.requestJustification(attendance) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    val justification = state.justificationFor
    if (justification != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelJustification,
            title = { Text(stringResource(R.string.attendance_justification_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.attendance_justification_help))
                    OutlinedTextField(
                        value = state.justification,
                        onValueChange = viewModel::onJustificationChange,
                        label = { Text(stringResource(R.string.attendance_justification_label)) },
                        supportingText = { Text("${state.justification.length}/500") },
                        minLines = 3,
                        maxLines = 6
                    )
                    if (state.error != null) Text(state.error!!.asString(), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitJustification, enabled = state.justification.isNotBlank() && !state.isSubmitting) {
                    Text(stringResource(R.string.attendance_justification_send))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelJustification) { Text(stringResource(R.string.attendance_cancel)) } }
        )
    }
}

@Composable
private fun AttendanceHistoryCard(attendance: Attendance, canJustify: Boolean, onJustify: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatInstant(attendance.clockIn), style = MaterialTheme.typography.titleMedium)
            val statusText = attendance.status?.let { status ->
                statusLabel(status)
            } ?: stringResource(R.string.attendance_status_unknown)

            Text(stringResource(R.string.attendance_status, statusText))
            attendance.clockOut?.let { Text(stringResource(R.string.attendance_clock_out_at, formatInstant(it))) }
            attendance.workedMinutes?.let { Text(stringResource(R.string.attendance_worked_minutes, it)) }
            attendance.regularMinutes?.let { Text(stringResource(R.string.attendance_regular_minutes, it)) }
            attendance.overtimeMinutes?.let { Text(stringResource(R.string.attendance_overtime_minutes, it)) }
            attendance.justificationReason?.let { Text(stringResource(R.string.attendance_justification_reason, it)) }
            attendance.justificationStatus?.let { Text(stringResource(R.string.attendance_justification_status, justificationLabel(it))) }
            if (canJustify && (attendance.status == AttendanceStatus.LATE || attendance.status == AttendanceStatus.ABSENT) && attendance.justificationStatus == null) {
                AtelierButton(text = stringResource(R.string.attendance_justify), onClick = onJustify, style = AtelierButtonStyle.Outlined, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun formatInstant(instant: Instant): String = DateFormatter.format(instant.atZone(LimaZone))

@Composable
private fun dayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
    DayOfWeek.TUESDAY -> stringResource(R.string.day_tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wednesday)
    DayOfWeek.THURSDAY -> stringResource(R.string.day_thursday)
    DayOfWeek.FRIDAY -> stringResource(R.string.day_friday)
    DayOfWeek.SATURDAY -> stringResource(R.string.day_saturday)
    DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
}

@Composable
private fun statusLabel(status: AttendanceStatus): String = when (status) {
    AttendanceStatus.ON_TIME -> stringResource(R.string.attendance_status_on_time)
    AttendanceStatus.LATE -> stringResource(R.string.attendance_status_late)
    AttendanceStatus.EXCUSED -> stringResource(R.string.attendance_status_excused)
    AttendanceStatus.ABSENT -> stringResource(R.string.attendance_status_absent)
}

@Composable
private fun justificationLabel(status: JustificationStatus): String = when (status) {
    JustificationStatus.PENDING -> stringResource(R.string.attendance_justification_pending)
    JustificationStatus.APPROVED -> stringResource(R.string.attendance_justification_approved)
    JustificationStatus.REJECTED -> stringResource(R.string.attendance_justification_rejected)
}
