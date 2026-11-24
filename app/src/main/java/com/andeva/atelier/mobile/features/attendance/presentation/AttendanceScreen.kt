package com.andeva.atelier.mobile.features.attendance.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSprings
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.EaseOutQuint
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.component.rememberReducedMotion
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.Branch
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.Geofence
import com.andeva.atelier.mobile.features.attendance.domain.GeofenceCheck
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule
import com.andeva.atelier.mobile.features.attendance.domain.WorkShift
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val LimaZone = ZoneId.of("America/Lima")
private val DateFormatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.forLanguageTag("es-PE"))
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))

/**
 * Pantalla de asistencia personal, geocerca GPS, turno semanal (US09) e historial con descargos (US10).
 * Cuando [onBack] es null se embebe dentro de la pestaña "Mi asistencia" de `AdminAttendanceScreen`.
 */
@Composable
fun AttendanceScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAtelierHaptics()

    var pendingClockIn by rememberSaveable { mutableStateOf(false) }
    var confirmClockOut by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingClockIn) {
                pendingClockIn = false
                viewModel.clockIn()
            } else {
                viewModel.previewLocation()
            }
        } else {
            pendingClockIn = false
            viewModel.onLocationPermissionDenied()
        }
    }

    fun requestLocation(forClockIn: Boolean) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (forClockIn) viewModel.clockIn() else viewModel.previewLocation()
        } else {
            pendingClockIn = forClockIn
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.previewLocation()
    }

    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }

    AttendanceContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onLocate = { requestLocation(forClockIn = false) },
        onClockIn = { requestLocation(forClockIn = true) },
        onRequestClockOut = { confirmClockOut = true },
        onRequestJustification = viewModel::requestJustification,
        onJustificationChange = viewModel::onJustificationChange,
        onCancelJustification = viewModel::cancelJustification,
        onSubmitJustification = viewModel::submitJustification,
        confirmClockOut = confirmClockOut,
        onDismissClockOut = { confirmClockOut = false },
        onConfirmClockOut = {
            confirmClockOut = false
            viewModel.clockOut()
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttendanceContent(
    state: AttendanceUiState,
    onBack: (() -> Unit)?,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onClockIn: () -> Unit,
    onRequestClockOut: () -> Unit,
    onRequestJustification: (Attendance) -> Unit,
    onJustificationChange: (String) -> Unit,
    onCancelJustification: () -> Unit,
    onSubmitJustification: () -> Unit,
    confirmClockOut: Boolean = false,
    onDismissClockOut: () -> Unit = {},
    onConfirmClockOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val showSkeleton = rememberDelayedLoading(state.isRefreshing && state.schedule == null && state.history.isEmpty())
    val canClockInPerm = state.session?.has(Permission.ATTENDANCE_CLOCK_IN) == true
    val canClockOutPerm = state.session?.has(Permission.ATTENDANCE_CLOCK_OUT) == true
    val canJustifyPerm = state.session?.has(Permission.JUSTIFICATIONS_CREATE) == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (onBack != null) {
            AtelierTopBar(
                title = stringResource(R.string.attendance_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.bays_refresh)
                        )
                    }
                }
            )
        }
        AtelierRefreshBar(visible = state.isRefreshing && !showSkeleton)

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && !showSkeleton,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.notice?.let { notice ->
                    item(key = "notice") {
                        AtelierBanner(
                            message = notice.asString(),
                            icon = Icons.Outlined.CheckCircle,
                            kind = BannerKind.Ok,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                state.error?.let { error ->
                    item(key = "error") {
                        AtelierBanner(
                            message = error.asString(),
                            icon = Icons.Outlined.ErrorOutline,
                            kind = BannerKind.Bad,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                state.locationWarning?.let { warning ->
                    item(key = "loc-warning") {
                        AtelierBanner(
                            message = warning.asString(),
                            icon = Icons.Outlined.WarningAmber,
                            kind = BannerKind.Wait,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item(key = "shift-status") {
                    ShiftAndGeofenceCard(
                        state = state,
                        loading = showSkeleton,
                        canClockInPerm = canClockInPerm,
                        canClockOutPerm = canClockOutPerm,
                        onLocate = onLocate,
                        onClockIn = onClockIn,
                        onRequestClockOut = onRequestClockOut
                    )
                }

                item(key = "schedule-header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.attendance_schedule_section),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item(key = "schedule-body") {
                    val schedule = state.schedule
                    if (showSkeleton) {
                        AtelierGroupedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            repeat(3) { idx ->
                                AtelierListRow(
                                    title = "Lunes",
                                    subtitle = "08:00 - 17:00",
                                    loading = true,
                                    showDivider = idx < 2
                                )
                            }
                        }
                    } else if (schedule == null) {
                        AtelierEmptyState(text = stringResource(R.string.attendance_no_branch_assigned))
                    } else {
                        WeeklyScheduleCard(schedule = schedule)
                    }
                }

                item(key = "history-header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.attendance_history_section),
                        count = state.history.size.takeIf { it > 0 },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                when {
                    showSkeleton -> items(3, key = { "hist-bone-$it" }) {
                        AtelierListRow(title = "Lun 16 Nov · 08:04", subtitle = "8h 00m trabajados", loading = true)
                    }
                    state.history.isEmpty() -> item(key = "history-empty") {
                        AtelierEmptyState(text = stringResource(R.string.attendance_history_empty))
                    }
                    else -> items(state.history, key = { it.id.value }) { item ->
                        AttendanceHistoryRow(
                            attendance = item,
                            canJustify = canJustifyPerm,
                            onRequestJustification = { onRequestJustification(item) }
                        )
                    }
                }

                if (onBack == null) {
                    item(key = "bar-space") { Spacer(Modifier.floatingBarPadding()) }
                }
            }
        }
    }

    if (confirmClockOut) {
        AtelierBottomSheet(
            title = stringResource(R.string.attendance_clock_out_sheet_title),
            onDismiss = onDismissClockOut,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.attendance_clock_out_confirm),
                    onClick = onConfirmClockOut,
                    loading = state.isSubmitting,
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth()
                )
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = onDismissClockOut,
                    style = AtelierButtonStyle.Outlined,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Text(
                text = stringResource(R.string.attendance_clock_out_sheet_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    state.justificationFor?.let { target ->
        AtelierBottomSheet(
            title = stringResource(R.string.attendance_justification_sheet_title),
            onDismiss = onCancelJustification,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.attendance_justification_send),
                    onClick = onSubmitJustification,
                    enabled = state.justification.isNotBlank() && !state.isSubmitting,
                    loading = state.isSubmitting,
                    icon = Icons.Outlined.EditNote,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Text(
                text = DateFormatter.format(target.clockIn.atZone(LimaZone)),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            AtelierTextField(
                label = stringResource(R.string.attendance_justification_field_label),
                value = state.justification,
                onValueChange = onJustificationChange,
                placeholder = stringResource(R.string.attendance_justification_field_hint),
                enabled = !state.isSubmitting
            )
            Text(
                text = "${state.justification.length} / 500",
                style = MaterialTheme.typography.labelMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

@Composable
private fun ShiftAndGeofenceCard(
    state: AttendanceUiState,
    loading: Boolean,
    canClockInPerm: Boolean,
    canClockOutPerm: Boolean,
    onLocate: () -> Unit,
    onClockIn: () -> Unit,
    onRequestClockOut: () -> Unit
) {
    val colors = AtelierTheme.colors
    val branch = state.schedule?.branch
    val shift = state.schedule?.shift
    val geofence = if (branch != null && state.deviceLocation != null) {
        Geofence.check(branch, state.deviceLocation)
    } else null

    val (badgeTextRes, badgeKind, badgeIcon) = when (state.today) {
        is TodayAttendance.ClockedIn -> Triple(R.string.floor_hub_shift_active, ChipKind.Ok, Icons.Outlined.CheckCircle)
        is TodayAttendance.ClockInQueued, is TodayAttendance.ClockOutQueued ->
            Triple(R.string.floor_hub_shift_pending, ChipKind.Idle, Icons.Outlined.Sync)
        is TodayAttendance.Completed -> Triple(R.string.floor_hub_shift_closed, ChipKind.Info, Icons.Outlined.Schedule)
        TodayAttendance.NotClockedIn -> Triple(R.string.floor_hub_shift_idle, ChipKind.Warn, Icons.Outlined.Schedule)
    }

    AtelierSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        tier = SurfaceTier.Elevated,
        shape = HeroShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = branch?.name ?: stringResource(R.string.dashboard_select_branch_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.bone(loading, widthFraction = 0.6f)
                    )
                    if (shift != null) {
                        Text(
                            text = "${shift.name} · ${TimeFormatter.format(shift.startTime)} – ${TimeFormatter.format(shift.endTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkSecondary,
                            modifier = Modifier.bone(loading, widthFraction = 0.75f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.today is TodayAttendance.ClockedIn && !loading) {
                        AtelierLiveBeacon(color = colors.okMark, pulsing = true)
                    }
                    AtelierChip(
                        text = stringResource(badgeTextRes),
                        kind = badgeKind,
                        icon = badgeIcon,
                        modifier = Modifier.bone(loading, widthFraction = 0.3f)
                    )
                }
            }

            // Estado de geocerca GPS con indicador radial geométrico
            val (geoText, geoKind) = when {
                geofence == null -> stringResource(R.string.attendance_geofence_pending) to ChipKind.Idle
                geofence.isMock -> stringResource(R.string.attendance_geofence_mock) to ChipKind.Bad
                geofence.isInside -> stringResource(
                    R.string.attendance_geofence_inside,
                    geofence.distanceMeters.toInt()
                ) to ChipKind.Ok
                else -> stringResource(
                    R.string.attendance_geofence_outside,
                    geofence.distanceMeters.toInt(),
                    branch?.geofenceRadiusMeters?.toInt() ?: 0
                ) to ChipKind.Bad
            }

            GeofenceRadarIndicator(
                geofence = geofence,
                radiusMeters = branch?.geofenceRadiusMeters,
                isLocating = state.isLocating,
                loading = loading,
                geoText = geoText,
                geoKind = geoKind,
                onLocate = onLocate
            )

            // Acciones principales en tamaño cómodo para fosa (56 dp)
            when {
                canClockInPerm && state.today is TodayAttendance.NotClockedIn -> {
                    AtelierButton(
                        text = stringResource(R.string.floor_hub_clock_in_cta),
                        onClick = onClockIn,
                        enabled = state.canClockIn,
                        loading = state.isSubmitting,
                        icon = Icons.AutoMirrored.Outlined.Login,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                }
                canClockOutPerm && state.canClockOut -> {
                    AtelierButton(
                        text = stringResource(R.string.floor_hub_clock_out_cta),
                        onClick = onRequestClockOut,
                        style = AtelierButtonStyle.Outlined,
                        enabled = state.canClockOut,
                        loading = state.isSubmitting,
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                }
                state.today is TodayAttendance.ClockOutQueued -> {
                    AtelierBanner(
                        message = stringResource(R.string.attendance_clock_out_queued),
                        icon = Icons.Outlined.Info,
                        kind = BannerKind.Info
                    )
                }
            }
        }
    }
}

/**
 * Indicador geométrico radial de geocerca GPS sobre [Canvas]: anillos concéntricos calibrados al
 * radio permitido de sede, vector de posición proporcional (`distanceMeters / geofenceRadiusMeters`)
 * y baliza central [AtelierLiveBeacon], respetando [rememberReducedMotion].
 */
@Composable
private fun GeofenceRadarIndicator(
    geofence: GeofenceCheck?,
    radiusMeters: Double?,
    isLocating: Boolean,
    loading: Boolean,
    geoText: String,
    geoKind: ChipKind,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val reducedMotion = rememberReducedMotion()
    val statusColor = when {
        isLocating -> AtelierBlueSoft
        geofence == null -> colors.inkSecondary
        geofence.isMock || !geofence.isInside -> colors.badMark
        else -> colors.okMark
    }
    val rawRatio = when {
        geofence == null -> 0f
        radiusMeters != null && radiusMeters > 0.0 -> (geofence.distanceMeters / radiusMeters).toFloat()
        geofence.isInside -> 0.40f
        else -> 1.25f
    }
    val animatedRatio by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0f, 1.35f),
        animationSpec = if (reducedMotion) tween(0) else AtelierSprings.criticallyDamped(),
        label = "geofenceRatio"
    )
    val locatingSweep: Float = if (isLocating && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "geofenceSweep")
        val sweep by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = EaseOutQuint),
                repeatMode = RepeatMode.Restart
            ),
            label = "geofenceSweepRadius"
        )
        sweep
    } else {
        0f
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f - 4.dp.toPx()
                val geofenceRadius = outerRadius * 0.72f
                val crosshairStroke = 1.dp.toPx()

                // Ejes de referencia ortogonales de instrumento
                drawLine(
                    color = colors.outlineSoft,
                    start = Offset(center.x - outerRadius, center.y),
                    end = Offset(center.x + outerRadius, center.y),
                    strokeWidth = crosshairStroke
                )
                drawLine(
                    color = colors.outlineSoft,
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y + outerRadius),
                    strokeWidth = crosshairStroke
                )

                // Anillos concéntricos de escala (50 % interno y límite exterior)
                drawCircle(
                    color = colors.outlineSoft,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = colors.outlineSoft,
                    radius = geofenceRadius * 0.5f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Anillo del radio permitido de sede
                val boundaryStroke = if (geofence?.isMock == true) {
                    Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                } else {
                    Stroke(width = 1.5.dp.toPx())
                }
                drawCircle(
                    color = statusColor.copy(alpha = 0.08f),
                    radius = geofenceRadius,
                    center = center
                )
                drawCircle(
                    color = statusColor.copy(alpha = 0.65f),
                    radius = geofenceRadius,
                    center = center,
                    style = boundaryStroke
                )

                if (isLocating && !reducedMotion) {
                    drawCircle(
                        color = AtelierBlueSoft.copy(alpha = (1f - locatingSweep) * 0.45f),
                        radius = geofenceRadius * locatingSweep,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Punto de posición proporcional a distanceMeters / geofenceRadiusMeters
                if (geofence != null && !loading) {
                    val dotDistance = if (animatedRatio <= 1f) {
                        geofenceRadius * animatedRatio
                    } else {
                        val overflow = ((animatedRatio - 1f) / 0.35f).coerceIn(0f, 1f)
                        val minOutside = geofenceRadius + 4.dp.toPx()
                        val maxOutside = outerRadius - 3.dp.toPx()
                        minOutside + (maxOutside - minOutside) * overflow
                    }
                    val bearingRad = Math.toRadians(-38.0)
                    val dotCenter = Offset(
                        x = center.x + dotDistance * cos(bearingRad).toFloat(),
                        y = center.y + dotDistance * sin(bearingRad).toFloat()
                    )
                    drawLine(
                        color = statusColor.copy(alpha = 0.55f),
                        start = center,
                        end = dotCenter,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = statusColor.copy(alpha = 0.24f),
                        radius = 8.dp.toPx(),
                        center = dotCenter
                    )
                    drawCircle(
                        color = statusColor,
                        radius = 4.dp.toPx(),
                        center = dotCenter
                    )
                }
            }

            AtelierLiveBeacon(
                color = statusColor,
                size = 7.dp,
                pulsing = isLocating || (geofence != null && geofence.isInside && !geofence.isMock)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AtelierChip(
                text = if (isLocating) stringResource(R.string.pit_geofence_locating) else geoText,
                kind = if (isLocating) ChipKind.Info else geoKind,
                icon = Icons.Outlined.GpsFixed
            )
            if (radiusMeters != null) {
                Text(
                    text = stringResource(R.string.attendance_geofence_radius, radiusMeters.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )
            }
            if (geofence != null && radiusMeters != null && radiusMeters > 0.0 && !loading) {
                AtelierMetricBar(
                    progress = (geofence.distanceMeters / radiusMeters).toFloat().coerceIn(0f, 1f),
                    color = statusColor,
                    markerAt = 1f
                )
            }
            AtelierButton(
                text = stringResource(R.string.attendance_retry_location),
                onClick = onLocate,
                style = AtelierButtonStyle.Text,
                loading = isLocating,
                icon = Icons.Outlined.MyLocation
            )
        }
    }
}

@Composable
private fun WeeklyScheduleCard(schedule: WeeklySchedule) {
    val shift = schedule.shift
    val locale = Locale.forLanguageTag("es-PE")
    AtelierGroupedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        AtelierGroupedRow(
            title = shift.name,
            subtitle = stringResource(R.string.attendance_schedule_tolerance, shift.gracePeriodMinutes),
            icon = Icons.Outlined.Schedule,
            showChevron = false,
            showDivider = true
        )
        schedule.days.forEachIndexed { index, day ->
            val dayName = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.titlecase(locale) }
            AtelierListRow(
                title = dayName,
                subtitle = if (day.isWorkDay) {
                    "${TimeFormatter.format(shift.startTime)} – ${TimeFormatter.format(shift.endTime)}"
                } else null,
                showDivider = index < schedule.days.lastIndex,
                trailing = {
                    AtelierChip(
                        text = stringResource(
                            if (day.isWorkDay) R.string.attendance_schedule_workday
                            else R.string.attendance_schedule_restday
                        ),
                        kind = if (day.isWorkDay) ChipKind.Info else ChipKind.Idle
                    )
                }
            )
        }
    }
}

@Composable
private fun AttendanceHistoryRow(
    attendance: Attendance,
    canJustify: Boolean,
    onRequestJustification: () -> Unit
) {
    val clockInLabel = DateFormatter.format(attendance.clockIn.atZone(LimaZone))
    val clockOutLabel = attendance.clockOut?.let { TimeFormatter.format(it.atZone(LimaZone)) }
    val workedPart = attendance.workedMinutes?.let { mins ->
        val base = stringResource(R.string.attendance_worked_summary, mins / 60, mins % 60)
        val extra = attendance.overtimeMinutes?.takeIf { it > 0 }?.let {
            stringResource(R.string.attendance_overtime_summary, it)
        }.orEmpty()
        base + extra
    }
    val subtitle = listOfNotNull(
        clockOutLabel?.let { "Salida $it" },
        workedPart,
        attendance.justificationStatus?.let { status ->
            when (status) {
                JustificationStatus.PENDING -> "Descargo pendiente"
                JustificationStatus.APPROVED -> "Descargo aprobado"
                JustificationStatus.REJECTED -> "Descargo rechazado"
            }
        }
    ).joinToString(" · ").ifEmpty { null }

    val (chipLabel, chipKind) = when (attendance.status) {
        AttendanceStatus.ON_TIME -> "A tiempo" to ChipKind.Ok
        AttendanceStatus.LATE -> "Tardanza" to ChipKind.Warn
        AttendanceStatus.ABSENT -> "Ausente" to ChipKind.Bad
        AttendanceStatus.EXCUSED -> "Justificado" to ChipKind.Info
        null -> "Registrado" to ChipKind.Idle
    }
    val needsJustification = canJustify &&
        (attendance.status == AttendanceStatus.LATE || attendance.status == AttendanceStatus.ABSENT) &&
        attendance.justificationStatus == null

    Column(modifier = Modifier.fillMaxWidth()) {
        AtelierListRow(
            title = clockInLabel,
            subtitle = subtitle,
            showDivider = !needsJustification,
            trailing = { AtelierChip(text = chipLabel, kind = chipKind) }
        )
        if (needsJustification) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AtelierButton(
                    text = stringResource(R.string.attendance_justification_send),
                    onClick = onRequestJustification,
                    style = AtelierButtonStyle.Tonal,
                    icon = Icons.Outlined.EditNote
                )
            }
        }
    }
}

// region Previews

private val previewAttendanceState = AttendanceUiState(
    today = TodayAttendance.NotClockedIn,
    schedule = WeeklySchedule(
        branchId = "b1",
        shiftId = "s1",
        shift = WorkShift(
            id = "s1",
            name = "Turno Mañana",
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            gracePeriodMinutes = 10,
            workDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
            ),
            restDays = setOf(DayOfWeek.SUNDAY),
            isActive = true
        ),
        branch = Branch("b1", "Sede Chorrillos", -12.17, -77.01, 100.0, true)
    ),
    deviceLocation = DeviceLocation(-12.1701, -77.0101, 12f, isMock = false),
    history = listOf(
        Attendance(
            id = AttendanceId("a1"),
            branchId = "b1",
            shiftId = "s1",
            clockIn = Instant.parse("2026-11-16T13:18:00Z"),
            clockOut = Instant.parse("2026-11-16T22:05:00Z"),
            status = AttendanceStatus.LATE,
            distanceToBranchMeters = 18.0,
            workedMinutes = 527,
            regularMinutes = 480,
            overtimeMinutes = 47,
            justificationReason = null,
            justificationStatus = null
        )
    ),
    isRefreshing = false
)

@Preview(name = "Asistencia · claro", widthDp = 390, heightDp = 844)
@Composable
private fun AttendancePreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AttendanceContent(
        state = previewAttendanceState,
        onBack = {},
        onRefresh = {},
        onLocate = {},
        onClockIn = {},
        onRequestClockOut = {},
        onRequestJustification = {},
        onJustificationChange = {},
        onCancelJustification = {},
        onSubmitJustification = {}
    )
}

@Preview(name = "Asistencia · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttendancePreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AttendanceContent(
        state = previewAttendanceState,
        onBack = {},
        onRefresh = {},
        onLocate = {},
        onClockIn = {},
        onRequestClockOut = {},
        onRequestJustification = {},
        onJustificationChange = {},
        onCancelJustification = {},
        onSubmitJustification = {}
    )
}

// endregion
