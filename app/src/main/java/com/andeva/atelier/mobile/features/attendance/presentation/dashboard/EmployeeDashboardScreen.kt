package com.andeva.atelier.mobile.features.attendance.presentation.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.domain.Geofence
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LimaZone = ZoneId.of("America/Lima")
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))

/**
 * Dashboard operativo de colaborador / técnico mecánico (estilo Lemon Cash adaptado a Atelier).
 */
@Composable
fun EmployeeDashboardScreen(
    onOpenMenu: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTaskDetail: (taskId: String) -> Unit,
    onOpenObd: () -> Unit,
    onOpenReportFinding: (workOrderId: String) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenAttendanceDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmployeeDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClockOutConfirmation by remember { mutableStateOf(false) }
    var showNoOrdersDialog by remember { mutableStateOf(false) }
    var showOrderPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
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

    val canMarkIn = state.session?.has(Permission.ATTENDANCE_CLOCK_IN) == true &&
        state.canClockIn && (
            location == null || (geofence?.isInside == true && !location.isMock && location.accuracyMeters <= 50f)
        )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cabecera con menú lateral, nombre de usuario y badges
        EmployeeHeader(
            state = state,
            onOpenMenu = onOpenMenu,
            onOpenSyncQueue = onOpenSyncQueue,
            onOpenNotifications = onOpenNotifications
        )

        // Banners informativos o de error
        state.notice?.let { notice ->
            AtelierBanner(message = notice.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
        }
        state.error?.let { error ->
            AtelierBanner(message = error.asString(), icon = Icons.Filled.Error, kind = BannerKind.Bad)
        }

        // 2. Widget de Asistencia y Jornada Express
        AttendanceExpressCard(
            state = state,
            geofenceInside = geofence?.isInside == true,
            canMarkIn = canMarkIn,
            onClockIn = {
                if (!fineGranted) {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                } else {
                    viewModel.clockIn()
                }
            },
            onClockOut = { showClockOutConfirmation = true },
            onOpenAttendanceDetails = onOpenAttendanceDetails
        )

        // 3. Tarjeta Hero: Tarea en curso activa
        ActiveTaskHeroCard(
            activeTask = state.activeTask,
            pendingCount = state.pendingTasksCount,
            onOpenTaskDetail = onOpenTaskDetail,
            onOpenTasks = onOpenTasks
        )

        // 4. Métricas de la jornada
        EmployeeMetricsRow(
            completedCount = state.completedTasksCount,
            pendingCount = state.pendingTasksCount,
            activeObdCount = state.activeObdCount,
            onOpenTasks = onOpenTasks,
            onOpenObd = onOpenObd
        )

        // 5. Accesos rápidos operativos (Lemon Cash style)
        Text(
            text = stringResource(R.string.employee_quick_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
        )

        AtelierSelectionCard(
            title = stringResource(R.string.employee_action_obd),
            subtitle = stringResource(R.string.more_item_obd_sub),
            leadingIcon = bluetooth_searching,
            badgeText = if (state.activeObdCount > 0) "${state.activeObdCount} activos" else null,
            badgeColor = AtelierTheme.colors.ok.copy(alpha = 0.2f),
            onClick = onOpenObd
        )

        AtelierSelectionCard(
            title = stringResource(R.string.employee_action_report_finding),
            subtitle = stringResource(R.string.proposals_title),
            leadingIcon = Icons.Outlined.CameraAlt,
            onClick = {
                val orders = state.assignedWorkOrders
                when {
                    orders.isEmpty() -> showNoOrdersDialog = true
                    orders.size == 1 -> onOpenReportFinding(orders.first().workOrderId)
                    else -> showOrderPicker = true
                }
            }
        )

        AtelierSelectionCard(
            title = stringResource(R.string.employee_action_view_stock),
            subtitle = stringResource(R.string.more_item_catalog_sub),
            leadingIcon = Icons.Outlined.Inventory2,
            onClick = onOpenInventory
        )

        Spacer(modifier = Modifier.height(8.dp))

        AtelierButton(
            text = stringResource(R.string.bays_refresh),
            onClick = viewModel::refresh,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Text,
            loading = state.isRefreshing
        )
    }

    if (showClockOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showClockOutConfirmation = false },
            confirmButton = {
                AtelierButton(
                    text = stringResource(R.string.employee_clock_out_btn),
                    onClick = {
                        showClockOutConfirmation = false
                        viewModel.clockOut()
                    }
                )
            },
            dismissButton = {
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = { showClockOutConfirmation = false },
                    style = AtelierButtonStyle.Text
                )
            },
            title = { Text(stringResource(R.string.employee_clock_out_confirm_title)) },
            text = { Text(stringResource(R.string.employee_clock_out_confirm_msg)) }
        )
    }

    if (showNoOrdersDialog) {
        AlertDialog(
            onDismissRequest = { showNoOrdersDialog = false },
            title = { Text(stringResource(R.string.employee_finding_no_orders_title)) },
            text = { Text(stringResource(R.string.employee_finding_no_orders_message)) },
            confirmButton = {
                AtelierButton(
                    text = stringResource(R.string.employee_dialog_understood),
                    onClick = { showNoOrdersDialog = false }
                )
            }
        )
    }

    if (showOrderPicker) {
        AlertDialog(
            onDismissRequest = { showOrderPicker = false },
            title = { Text(stringResource(R.string.employee_finding_select_order_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.employee_finding_select_order_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    state.assignedWorkOrders.forEach { task ->
                        AtelierSelectionCard(
                            title = "OT #${task.workOrderNumber ?: task.workOrderId.take(8)} · ${task.plate ?: stringResource(R.string.register_customer_section_vehicle)}",
                            subtitle = task.serviceName ?: task.description,
                            leadingIcon = Icons.Outlined.DirectionsCar,
                            onClick = {
                                showOrderPicker = false
                                onOpenReportFinding(task.workOrderId)
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = { showOrderPicker = false },
                    style = AtelierButtonStyle.Text
                )
            }
        )
    }
}

@Composable
private fun EmployeeHeader(
    state: EmployeeDashboardUiState,
    onOpenMenu: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu, modifier = Modifier.padding(end = 4.dp)) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.more_hub_title),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.tenantName.ifBlank { stringResource(R.string.employee_dashboard_title) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.fullName.ifBlank { "Técnico operativo" },
                style = MaterialTheme.typography.bodySmall,
                color = AtelierTheme.colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (state.pendingSyncCount > 0) {
            IconButton(onClick = onOpenSyncQueue) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(state.pendingSyncCount.toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = stringResource(R.string.sync_queue_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        IconButton(onClick = onOpenNotifications) {
            BadgedBox(
                badge = {
                    if (state.unreadNotificationsCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(state.unreadNotificationsCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.notifications_title),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun AttendanceExpressCard(
    state: EmployeeDashboardUiState,
    geofenceInside: Boolean,
    canMarkIn: Boolean,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onOpenAttendanceDetails: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.employee_shift_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }
                when (val today = state.today) {
                    is TodayAttendance.ClockedIn -> {
                        AtelierChip(
                            text = "Ingreso: ${formatInstant(today.attendance.clockIn)}",
                            kind = ChipKind.Ok
                        )
                    }
                    is TodayAttendance.ClockInQueued -> {
                        AtelierChip(text = "Ingreso en cola", kind = ChipKind.Wait)
                    }
                    is TodayAttendance.ClockOutQueued -> {
                        AtelierChip(text = "Salida en cola", kind = ChipKind.Wait)
                    }
                    is TodayAttendance.Completed -> {
                        AtelierChip(text = stringResource(R.string.employee_attendance_status_out), kind = ChipKind.Idle)
                    }
                    TodayAttendance.NotClockedIn -> {
                        AtelierChip(
                            text = stringResource(R.string.employee_attendance_status_pending),
                            kind = ChipKind.Wait
                        )
                    }
                }
            }

            // Horario y geocerca
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val shiftDesc = state.schedule?.let { s ->
                        "${s.shift.name} · ${s.shift.startTime} a ${s.shift.endTime}"
                    } ?: stringResource(R.string.employee_shift_not_assigned)

                    Text(
                        text = shiftDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = state.branchName,
                        style = MaterialTheme.typography.bodySmall,
                        color = atelier.inkSecondary
                    )
                }

                if (geofenceInside) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(shape = CircleShape, color = atelier.ok, modifier = Modifier.size(8.dp)) {}
                        Text(
                            text = "En sede",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = atelier.ok
                        )
                    }
                }
            }

            // Acciones de marcación
            if (state.canClockIn) {
                AtelierButton(
                    text = stringResource(R.string.employee_clock_in_btn),
                    onClick = onClockIn,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSubmittingAttendance,
                    enabled = canMarkIn
                )
            } else if (state.canClockOut) {
                AtelierButton(
                    text = stringResource(R.string.employee_clock_out_btn),
                    onClick = onClockOut,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Tonal,
                    loading = state.isSubmittingAttendance
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAttendanceDetails)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.employee_action_attendance_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveTaskHeroCard(
    activeTask: Task?,
    pendingCount: Int,
    onOpenTaskDetail: (String) -> Unit,
    onOpenTasks: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors

    if (activeTask != null) {
        val isPaused = activeTask.isPausedByMechanic || activeTask.isSuspended
        val statusText = if (isPaused) "En pausa" else "En labor técnica"
        val statusKind = if (isPaused) ChipKind.Wait else ChipKind.Ok

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AtelierChip(text = statusText, kind = statusKind)
                    activeTask.bayName?.let { bay ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scheme.surface,
                            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = bay,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.primary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = activeTask.plate ?: "Vehículo en taller",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = activeTask.serviceName ?: activeTask.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = atelier.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                activeTask.startedAt?.let { started ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = atelier.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Iniciada a las ${formatInstant(started)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = atelier.inkSecondary
                        )
                    }
                }

                AtelierButton(
                    text = "Continuar labor técnica",
                    onClick = { onOpenTaskDetail(activeTask.id.value) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.employee_active_task_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.employee_no_active_task),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface
                )
                Text(
                    text = stringResource(R.string.employee_no_active_task_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = atelier.inkSecondary
                )

                AtelierButton(
                    text = stringResource(R.string.employee_see_all_tasks) + if (pendingCount > 0) " ($pendingCount)" else "",
                    onClick = onOpenTasks,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Tonal
                )
            }
        }
    }
}

@Composable
private fun EmployeeMetricsRow(
    completedCount: Int,
    pendingCount: Int,
    activeObdCount: Int,
    onOpenTasks: () -> Unit,
    onOpenObd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            title = stringResource(R.string.employee_kpi_completed_today),
            value = completedCount.toString(),
            color = AtelierTheme.colors.ok,
            onClick = onOpenTasks,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        MetricCard(
            title = stringResource(R.string.employee_kpi_pending_tasks),
            value = pendingCount.toString(),
            color = MaterialTheme.colorScheme.primary,
            onClick = onOpenTasks,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        MetricCard(
            title = stringResource(R.string.main_tab_obd),
            value = activeObdCount.toString(),
            color = AtelierTheme.colors.info,
            onClick = onOpenObd,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = AtelierTheme.colors.inkSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatInstant(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))
        .withZone(LimaZone)
        .format(instant)
