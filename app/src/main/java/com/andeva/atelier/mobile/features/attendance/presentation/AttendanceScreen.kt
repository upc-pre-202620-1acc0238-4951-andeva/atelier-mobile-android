package com.andeva.atelier.mobile.features.attendance.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.Branch
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.Geofence
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

private val LimaZone = ZoneId.of("America/Lima")
private val DateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-PE"))
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))

@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.previewLocation()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    LaunchedEffect(fineGranted) {
        if (fineGranted && state.deviceLocation == null) viewModel.previewLocation()
    }

    val schedule = state.schedule
    val branch = schedule?.branch
    val location = state.deviceLocation
    val geofence = if (branch != null && location != null) Geofence.check(branch, location) else null
    val colors = AtelierTheme.colors
    val isQueued = state.today is TodayAttendance.ClockInQueued || state.today is TodayAttendance.ClockOutQueued
    val hasClockedIn = state.today is TodayAttendance.ClockedIn || state.today is TodayAttendance.ClockInQueued
    val canMarkIn = state.session?.has(Permission.ATTENDANCE_CLOCK_IN) == true && state.canClockIn
    val canMarkOut = state.session?.has(Permission.ATTENDANCE_CLOCK_OUT) == true && state.canClockOut

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.attendance_title), style = MaterialTheme.typography.titleLarge)
                        state.session?.tenantName?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusPill(
                        text = when {
                            state.isLocating -> stringResource(R.string.attendance_locating)
                            state.isRefreshing -> stringResource(R.string.attendance_refreshing_short)
                            isQueued -> stringResource(R.string.attendance_pending_badge)
                            state.today is TodayAttendance.Completed -> stringResource(R.string.attendance_completed_badge)
                            hasClockedIn -> stringResource(R.string.attendance_active_badge)
                            else -> stringResource(R.string.attendance_ready_badge)
                        },
                        background = when {
                            isQueued -> colors.waitContainer
                            state.error != null -> colors.badContainer
                            else -> colors.okContainer
                        },
                        foreground = when {
                            isQueued -> colors.wait
                            state.error != null -> colors.bad
                            else -> colors.ok
                        }
                    )
                }
            }

            if (state.error != null) item { MessageBanner(state.error!!.asString(), colors.badContainer, colors.bad) }
            if (state.notice != null) item { MessageBanner(state.notice!!.asString(), colors.infoContainer, colors.primaryText) }
            if (state.locationWarning != null) item { MessageBanner(state.locationWarning!!.asString(), colors.waitContainer, colors.wait) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(branch?.name ?: stringResource(R.string.attendance_geofence_title), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    branch?.let { stringResource(R.string.attendance_geofence_radius, it.geofenceRadiusMeters.toInt()) }
                                        ?: stringResource(R.string.attendance_geofence_no_branch),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (fineGranted && !state.isLocating) {
                                TextButton(onClick = viewModel::previewLocation) { Text(stringResource(R.string.attendance_retry_location)) }
                            }
                        }
                        GeofenceMap(branch = branch, location = location)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    geofence?.let { stringResource(R.string.attendance_distance_value, it.distanceMeters.toInt()) } ?: "— m",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(stringResource(R.string.attendance_distance_from_branch), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(
                                text = when {
                                    location == null -> stringResource(R.string.attendance_gps_unavailable_short)
                                    location.isMock -> stringResource(R.string.attendance_mock_short)
                                    geofence == null -> stringResource(R.string.attendance_gps_ready)
                                    geofence.isInside -> stringResource(R.string.attendance_inside_geofence)
                                    else -> stringResource(R.string.attendance_outside_geofence)
                                },
                                background = when {
                                    geofence?.isInside == true && location?.isMock == false -> colors.okContainer
                                    geofence?.isInside == false || location?.isMock == true -> colors.badContainer
                                    else -> colors.idleContainer
                                },
                                foreground = when {
                                    geofence?.isInside == true && location?.isMock == false -> colors.ok
                                    geofence?.isInside == false || location?.isMock == true -> colors.bad
                                    else -> colors.muted
                                }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconTile { Icon(Icons.Filled.Schedule, contentDescription = null) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.attendance_my_shift), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    schedule?.let { "${it.shift.startTime.format(TimeFormatter)} – ${it.shift.endTime.format(TimeFormatter)}" }
                                        ?: stringResource(R.string.attendance_no_shift_short),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconTile { Icon(Icons.Filled.AccessTime, contentDescription = null) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.attendance_grace_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    schedule?.let { stringResource(R.string.attendance_grace_period, it.shift.gracePeriodMinutes) }
                                        ?: stringResource(R.string.attendance_no_shift_short),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (schedule != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconTile { Icon(Icons.Filled.Work, contentDescription = null) }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(R.string.attendance_branch_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(schedule.branch?.name ?: stringResource(R.string.attendance_branch_unavailable), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (schedule != null) {
                item {
                    Text(stringResource(R.string.attendance_weekly_schedule), style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            schedule.days.forEachIndexed { index, day ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dayName(day.dayOfWeek), style = MaterialTheme.typography.bodyMedium)
                                    StatusPill(
                                        text = if (day.isWorkDay) stringResource(R.string.attendance_work_day) else stringResource(R.string.attendance_rest_day),
                                        background = if (day.isWorkDay) colors.infoContainer else colors.idleContainer,
                                        foreground = if (day.isWorkDay) colors.primaryText else colors.muted
                                    )
                                }
                                if (index != schedule.days.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.attendance_today), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::refresh, enabled = !state.isRefreshing) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.attendance_refresh))
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            when (val today = state.today) {
                                TodayAttendance.NotClockedIn -> stringResource(R.string.attendance_not_clocked_in)
                                is TodayAttendance.ClockInQueued -> stringResource(R.string.attendance_clock_in_pending)
                                is TodayAttendance.ClockedIn -> stringResource(R.string.attendance_clocked_in_at, formatInstant(today.attendance.clockIn))
                                is TodayAttendance.ClockOutQueued -> stringResource(R.string.attendance_clock_out_pending)
                                is TodayAttendance.Completed -> stringResource(R.string.attendance_completed_at, formatInstant(today.attendance.clockOut ?: today.attendance.clockIn))
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        when (val today = state.today) {
                            is TodayAttendance.ClockInQueued -> Text(stringResource(R.string.attendance_captured_at, formatInstant(today.capturedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            is TodayAttendance.ClockOutQueued -> Text(stringResource(R.string.attendance_captured_at, formatInstant(today.capturedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            is TodayAttendance.ClockedIn -> Text(stringResource(R.string.attendance_distance, today.attendance.distanceToBranchMeters.toInt()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            is TodayAttendance.Completed -> Text(stringResource(R.string.attendance_worked_minutes, today.attendance.workedMinutes ?: 0L), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TodayAttendance.NotClockedIn -> Unit
                        }
                        if (state.session?.has(Permission.ATTENDANCE_CLOCK_IN) == true && state.today is TodayAttendance.NotClockedIn) {
                            AtelierButton(
                                text = if (state.isLocating) stringResource(R.string.attendance_locating) else stringResource(R.string.attendance_clock_in),
                                onClick = {
                                    when {
                                        !fineGranted -> locationLauncher.launch(locationPermissions)
                                        location == null -> viewModel.previewLocation()
                                        else -> viewModel.clockIn()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canMarkIn && !state.isLocating,
                                loading = state.isSubmitting || state.isLocating
                            )
                        }
                        if (state.session?.has(Permission.ATTENDANCE_CLOCK_OUT) == true && (state.today is TodayAttendance.ClockedIn || state.today is TodayAttendance.ClockInQueued)) {
                            AtelierButton(
                                text = stringResource(R.string.attendance_clock_out),
                                onClick = viewModel::clockOut,
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.Tonal,
                                enabled = canMarkOut,
                                loading = state.isSubmitting
                            )
                        }
                        if (state.today is TodayAttendance.Completed) {
                            Text(stringResource(R.string.attendance_completed_badge), color = colors.ok, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Text(stringResource(R.string.attendance_history), style = MaterialTheme.typography.titleMedium) }
            if (state.history.isEmpty()) item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(stringResource(R.string.attendance_history_empty), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.history, key = { it.id.value }) { attendance ->
                AttendanceHistoryCard(
                    attendance = attendance,
                    canJustify = state.session?.has(Permission.JUSTIFICATIONS_CREATE) == true,
                    onJustify = { viewModel.requestJustification(attendance) }
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
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
private fun GeofenceMap(branch: Branch?, location: DeviceLocation?) {
    val colors = AtelierTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val road = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier.fillMaxWidth().height(174.dp).clip(RoundedCornerShape(14.dp)).background(surface)
    ) {
        val block = size.width / 5f
        for (row in 0..2) {
            for (col in 0..4) {
                val left = col * block + 3.dp.toPx()
                val top = row * (size.height / 3f) + 3.dp.toPx()
                drawRoundRect(
                    color = road,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(block - 6.dp.toPx(), size.height / 3f - 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            }
        }
        drawRect(color = road, topLeft = Offset(0f, size.height * .43f), size = androidx.compose.ui.geometry.Size(size.width, 12.dp.toPx()))
        drawRect(color = road, topLeft = Offset(size.width * .42f, 0f), size = androidx.compose.ui.geometry.Size(12.dp.toPx(), size.height))
        val center = Offset(size.width * .52f, size.height * .52f)
        val radius = min(size.height * .43f, size.width * .21f)
        drawCircle(color = primary.copy(alpha = .12f), radius = radius, center = center)
        drawCircle(
            color = primary,
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())))
        )
        drawCircle(color = primary, radius = 13.dp.toPx(), center = center)
        drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 4.dp.toPx(), center = center)
        if (branch != null && location != null) {
            val dLat = Math.toRadians(location.latitude - branch.latitude)
            val dLon = Math.toRadians(location.longitude - branch.longitude) * cos(Math.toRadians(branch.latitude))
            val distance = max(1.0, branch.geofenceRadiusMeters)
            val scale = radius / distance.toFloat()
            val point = Offset(
                x = center.x + (dLon * 6_371_008.8 * scale).toFloat(),
                y = center.y - (dLat * 6_371_008.8 * scale).toFloat()
            )
            drawCircle(
                color = if (location.isMock) colors.bad else colors.ok,
                radius = 7.dp.toPx(),
                center = Offset(point.x.coerceIn(10.dp.toPx(), size.width - 10.dp.toPx()), point.y.coerceIn(10.dp.toPx(), size.height - 10.dp.toPx()))
            )
            drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 2.5.dp.toPx(), center = Offset(point.x.coerceIn(10.dp.toPx(), size.width - 10.dp.toPx()), point.y.coerceIn(10.dp.toPx(), size.height - 10.dp.toPx())))
        }
    }
    Text(
        text = branch?.let { stringResource(R.string.attendance_map_radius, it.geofenceRadiusMeters.toInt()) }
            ?: stringResource(R.string.attendance_map_no_data),
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatusPill(text: String, background: androidx.compose.ui.graphics.Color, foreground: androidx.compose.ui.graphics.Color) {
    Surface(color = background, contentColor = foreground, shape = CircleShape) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MessageBanner(text: String, background: androidx.compose.ui.graphics.Color, foreground: androidx.compose.ui.graphics.Color) {
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(12.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun IconTile(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer,
            content = content
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
