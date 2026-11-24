package com.andeva.atelier.mobile.features.attendance.presentation.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierAdaptiveRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierDecisionRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalAtelierMinTouchTarget
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.attendance.domain.Branch
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule
import com.andeva.atelier.mobile.features.attendance.domain.WorkShift
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.tasks.domain.LaborTime
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HubDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-PE"))
private val HubTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))
private val HubLimaZone = ZoneId.of("America/Lima")

/**
 * Hub de Piso y Mostrador ("Turno") del Segmento 2.
 * Compone su panel de cabecera y bloques operativos exclusivamente desde los permisos de la sesión,
 * siguiendo el sistema visual "La vía señalizada" del Segmento 1.
 */
@Composable
fun FloorHubScreen(
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenAttendanceDetail: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTaskDetail: (workOrderId: String, taskId: String) -> Unit,
    onOpenObd: () -> Unit,
    onOpenReportFinding: (workOrderId: String) -> Unit,
    onOpenBays: () -> Unit,
    onOpenReception: () -> Unit,
    onRegisterWalkIn: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenPartDetail: (partId: String) -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenPurchaseOrders: () -> Unit,
    onOpenPurchaseOrderDetail: (purchaseOrderId: String) -> Unit,
    onOpenWorkOrderCheckout: (workOrderId: String) -> Unit,
    onOpenDailyCash: () -> Unit,
    viewModel: FloorHubViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAtelierHaptics()

    var confirmClockOut by rememberSaveable { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.clockIn() else viewModel.onLocationPermissionDenied()
    }

    fun triggerClockIn() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.clockIn() else locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }

    FloorHubContent(
        state = state,
        actions = FloorHubActions(
            onRefresh = viewModel::refresh,
            onClockIn = ::triggerClockIn,
            onRequestClockOut = { confirmClockOut = true },
            onOpenProfile = onOpenProfile,
            onOpenNotifications = onOpenNotifications,
            onOpenSyncQueue = onOpenSyncQueue,
            onOpenAttendanceDetail = onOpenAttendanceDetail,
            onOpenTasks = onOpenTasks,
            onOpenTaskDetail = onOpenTaskDetail,
            onOpenObd = onOpenObd,
            onOpenReportFinding = onOpenReportFinding,
            onOpenBays = onOpenBays,
            onOpenReception = onOpenReception,
            onRegisterWalkIn = onRegisterWalkIn,
            onOpenInventory = onOpenInventory,
            onOpenPartDetail = onOpenPartDetail,
            onOpenStockAlerts = onOpenStockAlerts,
            onOpenPurchaseOrders = onOpenPurchaseOrders,
            onOpenPurchaseOrderDetail = onOpenPurchaseOrderDetail,
            onOpenWorkOrderCheckout = onOpenWorkOrderCheckout,
            onOpenDailyCash = onOpenDailyCash
        )
    )

    if (confirmClockOut) {
        AtelierBottomSheet(
            title = stringResource(R.string.attendance_clock_out_sheet_title),
            onDismiss = { confirmClockOut = false },
            actions = {
                AtelierButton(
                    text = stringResource(R.string.attendance_clock_out_confirm),
                    onClick = {
                        confirmClockOut = false
                        viewModel.clockOut()
                    },
                    loading = state.isSubmittingAttendance,
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth()
                )
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = { confirmClockOut = false },
                    style = AtelierButtonStyle.Outlined,
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
}

internal data class FloorHubActions(
    val onRefresh: () -> Unit = {},
    val onClockIn: () -> Unit = {},
    val onRequestClockOut: () -> Unit = {},
    val onOpenProfile: () -> Unit = {},
    val onOpenNotifications: () -> Unit = {},
    val onOpenSyncQueue: () -> Unit = {},
    val onOpenAttendanceDetail: () -> Unit = {},
    val onOpenTasks: () -> Unit = {},
    val onOpenTaskDetail: (String, String) -> Unit = { _, _ -> },
    val onOpenObd: () -> Unit = {},
    val onOpenReportFinding: (String) -> Unit = {},
    val onOpenBays: () -> Unit = {},
    val onOpenReception: () -> Unit = {},
    val onRegisterWalkIn: () -> Unit = {},
    val onOpenInventory: () -> Unit = {},
    val onOpenPartDetail: (String) -> Unit = {},
    val onOpenStockAlerts: () -> Unit = {},
    val onOpenPurchaseOrders: () -> Unit = {},
    val onOpenPurchaseOrderDetail: (String) -> Unit = {},
    val onOpenWorkOrderCheckout: (String) -> Unit = {},
    val onOpenDailyCash: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FloorHubContent(state: FloorHubUiState, actions: FloorHubActions) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton)

    CompositionLocalProvider(LocalAtelierMinTouchTarget provides 56.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AtelierMainTopBar(
            title = state.tenantName.ifBlank { stringResource(R.string.app_name) },
            subtitle = state.fullName.takeIf { it.isNotBlank() },
            onOpenNotifications = actions.onOpenNotifications,
            unreadNotificationsCount = state.unreadNotificationsCount,
            onOpenSync = actions.onOpenSyncQueue,
            pendingSyncCount = state.pendingSyncCount,
            onOpenProfile = actions.onOpenProfile
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.hasLoaded,
            onRefresh = actions.onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "hero-panel") {
                    HubReportPanel(state = state, loading = showSkeleton, actions = actions)
                }

                if (!state.isOnline) {
                    item(key = "offline") {
                        AtelierBanner(
                            message = stringResource(R.string.today_offline_cache),
                            icon = Icons.Outlined.Info,
                            kind = BannerKind.Info,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
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

                // 1. Tira de marcación GPS personal (siempre visible para colaboradores con turno)
                if (state.canClockInPerm || state.canClockOutPerm || state.schedule != null) {
                    item(key = "shift-express") {
                        ShiftExpressCard(state = state, actions = actions)
                    }
                }

                // 2. Bloque de Fosa / Mecánico
                if (state.canTrackTasks) {
                    mechanicBlock(state = state, loading = showSkeleton, actions = actions)
                }

                // 3. Bloque de Mostrador / Recepcionista
                if (state.canUseCounter) {
                    counterBlock(state = state, loading = showSkeleton, actions = actions)
                }

                // 4. Bloque de Almacén / Encargado de Inventario
                if (state.canManageStock) {
                    warehouseBlock(state = state, loading = showSkeleton, actions = actions)
                }

                // 5. Bloque de Caja / Cajero
                if (state.canOperateCash) {
                    cashierBlock(state = state, loading = showSkeleton, actions = actions)
                }

                // 6. Accesos de piso para quien tiene herramientas adicionales
                if (state.canTrackTasks || state.canUseObd || state.canReadBays) {
                    item(key = "floor-shortcuts") {
                        FloorShortcutsCard(state = state, actions = actions)
                    }
                }

                item(key = "bar-space") { Spacer(Modifier.floatingBarPadding()) }
            }
        }
    }
    }
}

@Composable
private fun HubReportPanel(state: FloorHubUiState, loading: Boolean, actions: FloorHubActions) {
    val dateText = if (state.todayDate != LocalDate.MIN) HubDateFormatter.format(state.todayDate) else ""
    val contextLine = listOfNotNull(state.branchName, dateText.takeIf { it.isNotBlank() }).joinToString(" · ")
        .ifBlank { stringResource(R.string.main_tab_shift) }

    when {
        state.canTrackTasks -> {
            val legend = if (state.activeTask != null) {
                state.activeTask?.plate?.ifBlank { null } ?: stringResource(R.string.floor_hub_tasks_legend_active)
            } else {
                state.pendingTasksCount.toString()
            }
            val caption = if (state.activeTask != null) {
                state.activeTask?.description.orEmpty()
            } else {
                stringResource(R.string.floor_hub_tasks_caption_format, state.pendingTasksCount, state.completedTasksCount)
            }
            val figures = buildList {
                add(PanelFigure(state.pendingTasksCount.toString(), stringResource(R.string.floor_hub_figure_pending), actions.onOpenTasks))
                add(PanelFigure(state.inProgressTasksCount.toString(), stringResource(R.string.floor_hub_figure_in_progress), actions.onOpenTasks))
                if (state.canUseObd) {
                    add(PanelFigure(state.activeObdCount.toString(), stringResource(R.string.floor_hub_figure_obd), actions.onOpenObd))
                }
            }
            AtelierReportPanel(
                context = contextLine,
                legend = legend,
                caption = caption,
                figures = figures,
                loading = loading
            )
        }
        state.canManageStock -> {
            AtelierReportPanel(
                context = contextLine,
                legend = state.stockAlerts.size.toString(),
                caption = stringResource(R.string.floor_hub_stock_caption),
                figures = listOf(
                    PanelFigure(state.outOfStockCount.toString(), stringResource(R.string.floor_hub_figure_out_of_stock), actions.onOpenStockAlerts),
                    PanelFigure(state.issuedPurchaseOrders.size.toString(), stringResource(R.string.floor_hub_figure_open_pos), actions.onOpenPurchaseOrders),
                    PanelFigure(state.catalogSize.toString(), stringResource(R.string.floor_hub_figure_catalog), actions.onOpenInventory)
                ),
                loading = loading
            )
        }
        state.canOperateCash -> {
            AtelierReportPanel(
                context = contextLine,
                legend = state.readyToBillOrders.size.toString(),
                caption = stringResource(R.string.floor_hub_cash_caption),
                figures = listOf(
                    PanelFigure(
                        state.dailyCash?.totalCollected?.format() ?: "S/ 0.00",
                        stringResource(R.string.floor_hub_figure_collected),
                        actions.onOpenDailyCash
                    ),
                    PanelFigure(
                        (state.dailyCash?.paymentCount ?: 0).toString(),
                        stringResource(R.string.floor_hub_figure_payments),
                        actions.onOpenDailyCash
                    )
                ),
                loading = loading
            )
        }
        state.canUseCounter -> {
            val arrived = state.appointments.count { it.status == AppointmentStatus.ARRIVED }
            val waiting = state.appointments.count {
                it.status == AppointmentStatus.PENDING || it.status == AppointmentStatus.CONFIRMED
            }
            AtelierReportPanel(
                context = contextLine,
                legend = state.appointments.size.toString(),
                caption = stringResource(R.string.floor_hub_counter_caption),
                figures = listOf(
                    PanelFigure(waiting.toString(), stringResource(R.string.floor_hub_figure_waiting), actions.onOpenReception),
                    PanelFigure(arrived.toString(), stringResource(R.string.floor_hub_figure_arrived), actions.onOpenReception),
                    PanelFigure(state.branchOrders.size.toString(), stringResource(R.string.floor_hub_figure_orders))
                ),
                loading = loading
            )
        }
        else -> {
            val statusLabel = when (state.todayAttendance) {
                is TodayAttendance.ClockedIn -> stringResource(R.string.floor_hub_shift_active)
                is TodayAttendance.ClockInQueued, is TodayAttendance.ClockOutQueued -> stringResource(R.string.floor_hub_shift_pending)
                is TodayAttendance.Completed -> stringResource(R.string.floor_hub_shift_closed)
                TodayAttendance.NotClockedIn -> stringResource(R.string.floor_hub_shift_idle)
            }
            AtelierReportPanel(
                context = contextLine,
                legend = statusLabel,
                caption = state.schedule?.shift?.name ?: stringResource(R.string.attendance_title),
                loading = loading
            )
        }
    }
}

@Composable
private fun ShiftExpressCard(state: FloorHubUiState, actions: FloorHubActions) {
    val shift = state.schedule?.shift
    val (badgeRes, badgeKind, badgeIcon) = when (state.todayAttendance) {
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
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_my_shift_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (shift != null) {
                        Text(
                            text = "${shift.name} · ${HubTimeFormatter.format(shift.startTime)} – ${HubTimeFormatter.format(shift.endTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtelierTheme.colors.inkSecondary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.todayAttendance is TodayAttendance.ClockedIn) {
                        AtelierLiveBeacon(color = AtelierTheme.colors.okMark, pulsing = true)
                    }
                    AtelierChip(text = stringResource(badgeRes), kind = badgeKind, icon = badgeIcon)
                }
            }

            AtelierAdaptiveRow(equalWidthInRow = true) {
                when {
                    state.canClockInNow -> {
                        AtelierButton(
                            text = stringResource(R.string.floor_hub_clock_in_cta),
                            onClick = actions.onClockIn,
                            loading = state.isSubmittingAttendance,
                            icon = Icons.AutoMirrored.Outlined.Login,
                            modifier = Modifier.heightIn(min = 52.dp)
                        )
                    }
                    state.canClockOutNow -> {
                        AtelierButton(
                            text = stringResource(R.string.floor_hub_clock_out_cta),
                            onClick = actions.onRequestClockOut,
                            style = AtelierButtonStyle.Outlined,
                            loading = state.isSubmittingAttendance,
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            modifier = Modifier.heightIn(min = 52.dp)
                        )
                    }
                }
                AtelierButton(
                    text = stringResource(R.string.floor_hub_attendance_detail),
                    onClick = actions.onOpenAttendanceDetail,
                    style = AtelierButtonStyle.Tonal,
                    modifier = if (state.canClockInNow || state.canClockOutNow) Modifier.heightIn(min = 52.dp) else Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ActiveMechanicTaskCard(
    active: Task,
    onOpenTaskDetail: () -> Unit
) {
    val colors = AtelierTheme.colors
    val isRunning = active.status == TaskStatus.IN_PROGRESS
    val startedAt = active.startedAt
    val estimatedHours = active.estimatedHours
    val now by produceState(initialValue = Instant.now(), key1 = active.id, key2 = isRunning) {
        while (isRunning) {
            value = Instant.now()
            delay(30_000L)
        }
    }

    AtelierSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        tier = SurfaceTier.Elevated,
        shape = HeroShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                active.plate?.let { AtelierPlate(it, size = PlateSize.L) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = active.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    active.bayName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkSecondary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRunning) {
                        AtelierLiveBeacon(color = colors.info, pulsing = true)
                    }
                    AtelierChip(
                        text = stringResource(taskStatusLabelRes(active.status, active.holdReason)),
                        kind = taskStatusChipKind(active.status, active.holdReason)
                    )
                }
            }

            if (startedAt != null && estimatedHours != null && estimatedHours > BigDecimal.ZERO) {
                val elapsedSeconds = LaborTime.effectiveSeconds(startedAt, active.totalPausedSeconds, now)
                val estimatedSeconds = (estimatedHours.toDouble() * 3600.0).coerceAtLeast(1.0)
                val rawProgress = (elapsedSeconds.toDouble() / estimatedSeconds).toFloat()
                val progress = rawProgress.coerceIn(0f, 1f)
                val barColor = when {
                    rawProgress >= 1f -> colors.badMark
                    rawProgress >= 0.85f -> colors.warnMark
                    else -> MaterialTheme.colorScheme.primary
                }
                val elapsedHours = (elapsedSeconds / 3600).toInt()
                val elapsedMinutes = ((elapsedSeconds % 3600) / 60).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.pit_task_progress_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkSecondary
                        )
                        Text(
                            text = stringResource(
                                R.string.pit_task_elapsed_vs_est,
                                elapsedHours,
                                elapsedMinutes,
                                estimatedHours.stripTrailingZeros().toPlainString()
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    AtelierMetricBar(
                        progress = progress,
                        color = barColor,
                        markerAt = 1f
                    )
                }
            }

            AtelierButton(
                text = stringResource(R.string.floor_hub_open_task),
                onClick = onOpenTaskDetail,
                icon = Icons.Outlined.PlayArrow,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            )
        }
    }
}

private fun LazyListScope.mechanicBlock(state: FloorHubUiState, loading: Boolean, actions: FloorHubActions) {
    val active = state.activeTask
    if (active != null) {
        item(key = "mech-active-header") {
            AtelierSectionHeader(
                title = stringResource(R.string.floor_hub_active_task_section),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item(key = "mech-active-card") {
            ActiveMechanicTaskCard(
                active = active,
                onOpenTaskDetail = { actions.onOpenTaskDetail(active.workOrderId, active.id.value) }
            )
        }
    }

    item(key = "mech-tasks-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.floor_hub_tasks_queue_section),
            count = state.pendingTasksCount.takeIf { it > 0 },
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            action = {
                AtelierButton(
                    text = stringResource(R.string.today_see_all),
                    onClick = actions.onOpenTasks,
                    style = AtelierButtonStyle.Text
                )
            }
        )
    }
    when {
        loading -> items(3, key = { "task-bone-$it" }) {
            AtelierListRow(title = "Cambio de pastillas", subtitle = "Bahía 1", plate = "BKL482", loading = true)
        }
        state.assignedTasks.isEmpty() -> item(key = "mech-tasks-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.floor_hub_tasks_empty),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        else -> items(state.assignedTasks.take(5), key = { "task-${it.id.value}" }) { task ->
            AtelierListRow(
                title = task.description,
                subtitle = listOfNotNull(
                    task.bayName,
                    task.estimatedHours?.let { "Est. ${it.stripTrailingZeros().toPlainString()} h" }
                ).joinToString(" · ").ifEmpty { null },
                plate = task.plate,
                onClick = { actions.onOpenTaskDetail(task.workOrderId, task.id.value) },
                trailing = {
                    AtelierChip(
                        text = stringResource(taskStatusLabelRes(task.status, task.holdReason)),
                        kind = taskStatusChipKind(task.status, task.holdReason)
                    )
                }
            )
        }
    }
}

private fun LazyListScope.counterBlock(state: FloorHubUiState, loading: Boolean, actions: FloorHubActions) {
    item(key = "counter-actions") {
        AtelierGroupedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AtelierButton(
                    text = stringResource(R.string.floor_hub_counter_open_reception),
                    onClick = actions.onOpenReception,
                    icon = Icons.Outlined.DirectionsCar,
                    modifier = Modifier.fillMaxWidth()
                )
                AtelierButton(
                    text = stringResource(R.string.floor_hub_counter_new_customer),
                    onClick = actions.onRegisterWalkIn,
                    style = AtelierButtonStyle.Tonal,
                    icon = Icons.Outlined.PersonAdd,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    item(key = "counter-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.floor_hub_counter_section),
            count = state.appointments.size.takeIf { it > 0 },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    when {
        loading -> items(3, key = { "appt-bone-$it" }) {
            AtelierListRow(title = "09:30 · Cliente", subtitle = "Mantenimiento", plate = "F4T119", loading = true)
        }
        state.appointments.isEmpty() -> item(key = "counter-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.floor_hub_counter_empty),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        else -> items(state.appointments.take(6), key = { "appt-${it.id}" }) { appt ->
            val time = HubTimeFormatter.format(appt.scheduledAt.atZone(HubLimaZone))
            AtelierListRow(
                title = listOfNotNull(time, appt.customerName).joinToString(" · "),
                subtitle = appt.reason,
                plate = appt.plate,
                onClick = actions.onOpenReception,
                trailing = {
                    val (label, kind) = when (appt.status) {
                        AppointmentStatus.CONFIRMED -> "Confirmada" to ChipKind.Info
                        AppointmentStatus.ARRIVED -> "Llegó" to ChipKind.Ok
                        AppointmentStatus.CANCELED -> "Cancelada" to ChipKind.Bad
                        else -> "Pendiente" to ChipKind.Warn
                    }
                    AtelierChip(text = label, kind = kind)
                }
            )
        }
    }
}

private fun LazyListScope.warehouseBlock(state: FloorHubUiState, loading: Boolean, actions: FloorHubActions) {
    item(key = "stock-alerts-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.floor_hub_stock_decisions),
            count = state.stockAlerts.size.takeIf { it > 0 },
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            action = {
                AtelierButton(
                    text = stringResource(R.string.today_see_all),
                    onClick = actions.onOpenStockAlerts,
                    style = AtelierButtonStyle.Text
                )
            }
        )
    }
    when {
        loading -> items(2, key = { "alert-bone-$it" }) {
            AtelierListRow(title = "Filtro de aceite", subtitle = "SKU-001", loading = true)
        }
        state.stockAlerts.isEmpty() -> item(key = "stock-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.floor_hub_stock_empty),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        else -> items(state.stockAlerts.take(5), key = { "alert-${it.itemId}" }) { alert ->
            AtelierDecisionRow(
                title = alert.name,
                subtitle = "${alert.sku} · Stock ${alert.currentStock.stripTrailingZeros().toPlainString()} / mín. ${alert.minimumStock.stripTrailingZeros().toPlainString()}",
                amount = "Faltan ${alert.deficit.stripTrailingZeros().toPlainString()}",
                onClick = { actions.onOpenPartDetail(alert.itemId) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    item(key = "stock-pos-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.floor_hub_stock_pos_section),
            count = state.issuedPurchaseOrders.size.takeIf { it > 0 },
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            action = {
                AtelierButton(
                    text = stringResource(R.string.floor_hub_stock_open_catalog),
                    onClick = actions.onOpenInventory,
                    style = AtelierButtonStyle.Text
                )
            }
        )
    }
    if (state.issuedPurchaseOrders.isEmpty() && !loading) {
        item(key = "stock-pos-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.floor_hub_stock_pos_empty),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    } else {
        items(state.issuedPurchaseOrders.take(4), key = { "po-${it.id}" }) { po ->
            AtelierListRow(
                title = po.orderNumber ?: "OC ${po.id.take(8)}",
                subtitle = "${po.lines.size} ítems · ${po.totalCost?.format() ?: ""}",
                onClick = { actions.onOpenPurchaseOrderDetail(po.id) },
                leading = {
                    AtelierChip(text = "Emitida", kind = ChipKind.Info, icon = Icons.Outlined.ShoppingCart)
                }
            )
        }
    }
}

private fun LazyListScope.cashierBlock(state: FloorHubUiState, loading: Boolean, actions: FloorHubActions) {
    item(key = "cash-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.floor_hub_cash_decisions),
            count = state.readyToBillOrders.size.takeIf { it > 0 },
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            action = {
                AtelierButton(
                    text = stringResource(R.string.floor_hub_cash_open_daily),
                    onClick = actions.onOpenDailyCash,
                    style = AtelierButtonStyle.Text
                )
            }
        )
    }
    when {
        loading -> items(2, key = { "cash-bone-$it" }) {
            AtelierListRow(title = "Orden #1042", subtitle = "Cliente", plate = "BKL482", loading = true)
        }
        state.readyToBillOrders.isEmpty() -> item(key = "cash-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.floor_hub_cash_empty),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        else -> items(state.readyToBillOrders.take(6), key = { "bill-${it.id}" }) { order ->
            AtelierDecisionRow(
                title = order.customerName ?: "Orden #${order.internalNumber ?: order.id.take(6)}",
                subtitle = order.bayName ?: "Lista para comprobante y cobro",
                plate = order.plate,
                amount = order.totalAmount?.format(),
                onClick = { actions.onOpenWorkOrderCheckout(order.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun FloorShortcutsCard(state: FloorHubUiState, actions: FloorHubActions) {
    val activeWorkOrderId = state.activeTask?.workOrderId ?: state.assignedTasks.firstOrNull()?.workOrderId
    val rows = buildList<@Composable (Boolean) -> Unit> {
        if (state.canUseObd) {
            add { div ->
                AtelierGroupedRow(
                    title = stringResource(R.string.floor_hub_quick_obd),
                    subtitle = stringResource(R.string.floor_hub_quick_obd_sub),
                    icon = bluetooth_searching,
                    showDivider = div,
                    onClick = actions.onOpenObd
                )
            }
        }
        if (state.canReportFinding && activeWorkOrderId != null) {
            add { div ->
                AtelierGroupedRow(
                    title = stringResource(R.string.floor_hub_quick_finding),
                    subtitle = stringResource(R.string.floor_hub_quick_finding_sub),
                    icon = Icons.Outlined.ReportProblem,
                    showDivider = div,
                    onClick = { actions.onOpenReportFinding(activeWorkOrderId) }
                )
            }
        }
        if (state.canReadParts) {
            add { div ->
                AtelierGroupedRow(
                    title = stringResource(R.string.floor_hub_quick_parts),
                    subtitle = stringResource(R.string.floor_hub_quick_parts_sub),
                    icon = Icons.Outlined.Inventory2,
                    showDivider = div,
                    onClick = actions.onOpenInventory
                )
            }
        }
        if (state.canReadBays) {
            add { div ->
                AtelierGroupedRow(
                    title = stringResource(R.string.floor_hub_quick_bays),
                    subtitle = stringResource(R.string.floor_hub_quick_bays_sub),
                    icon = Icons.Outlined.Garage,
                    showDivider = div,
                    onClick = actions.onOpenBays
                )
            }
        }
    }
    if (rows.isEmpty()) return

    AtelierGroupedCard(
        title = stringResource(R.string.floor_hub_quick_section),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        rows.forEachIndexed { idx, row -> row(idx < rows.lastIndex) }
    }
}

// region Previews

private val previewMechanicState = FloorHubUiState(
    todayDate = LocalDate.of(2026, 11, 22),
    session = Session(
        userId = "u2",
        email = "andres@tallersuarez.pe",
        fullName = "Andrés Vílchez",
        tenantId = "t1",
        tenantName = "Taller Suárez",
        membershipId = "m2",
        permissions = setOf(
            "hr:attendance:clock_in", "hr:attendance:clock_out", "hr:attendance:read_own",
            "operations:tasks:read", "operations:tasks:track_time", "operations:proposals:create",
            "operations:bays:read", "inventory:parts:read", "iot:telemetry:ingest", "iot:devices:read"
        ),
        plan = null,
        branchId = "b1",
        shiftId = "s1"
    ),
    schedule = WeeklySchedule(
        branchId = "b1",
        shiftId = "s1",
        shift = WorkShift("s1", "Turno Mañana", LocalTime.of(8, 0), LocalTime.of(17, 0), 10, emptySet(), setOf(DayOfWeek.SUNDAY), true),
        branch = Branch("b1", "Sede Chorrillos", -12.17, -77.01, 100.0, true)
    ),
    todayAttendance = TodayAttendance.NotClockedIn,
    assignedTasks = listOf(
        Task(
            id = TaskId("t1"),
            workOrderId = "wo1",
            workOrderNumber = 1042,
            workOrderStatus = "IN_PROGRESS",
            vehicleId = "v1",
            plate = "BKL482",
            branchId = "b1",
            bayId = "bay1",
            bayName = "Bahía 2 · Elevador",
            serviceName = "Frenos",
            mechanicId = "m2",
            status = TaskStatus.IN_PROGRESS,
            description = "Cambio de pastillas y rectificado de discos delanteros",
            estimatedHours = BigDecimal("2.0"),
            holdReason = null,
            missingItemDescription = null,
            pendingSuspensionReason = null,
            startedAt = Instant.now().minusSeconds(4200),
            totalPausedSeconds = 0
        )
    ),
    activeObdCount = 2,
    hasLoaded = true
)

@Preview(name = "Turno Mecánico · claro", widthDp = 390, heightDp = 844)
@Composable
private fun FloorHubPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    FloorHubContent(previewMechanicState, FloorHubActions())
}

@Preview(name = "Turno Mecánico · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FloorHubPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    FloorHubContent(previewMechanicState, FloorHubActions())
}

// endregion
