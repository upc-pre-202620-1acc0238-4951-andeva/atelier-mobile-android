package com.andeva.atelier.mobile.features.main.presentation.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierDecisionRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierKpiCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ContextDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-PE"))

/** Cuántas órdenes muestra "Hoy" antes de mandar a la pestaña Órdenes. */
private const val ORDERS_PREVIEW = 6

/**
 * "Hoy": el parte de la sede para quien la supervisa. Una cifra a escala de señal (autos en el taller), lo que
 * espera una decisión en ámbar y las órdenes abiertas. Lo que se decide aquí se decide en hoja inferior.
 */
@Composable
fun AdminDashboardScreen(
    onSelectBranch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenProfile: () -> Unit,
    onReceiveVehicle: () -> Unit,
    onRegisterWalkIn: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenBays: () -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenTeam: () -> Unit,
    onOpenObd: () -> Unit,
    onOpenMyAttendance: () -> Unit = {},
    onOpenMyTasks: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminDashboardContent(
        state = state,
        actions = DashboardActions(
            onRefresh = viewModel::refresh,
            onSelectBranch = onSelectBranch,
            onOpenNotifications = onOpenNotifications,
            onOpenSyncQueue = onOpenSyncQueue,
            onOpenProfile = onOpenProfile,
            onReceiveVehicle = onReceiveVehicle,
            onRegisterWalkIn = onRegisterWalkIn,
            onOpenOrder = onOpenOrder,
            onOpenOrders = onOpenOrders,
            onOpenBays = onOpenBays,
            onOpenStockAlerts = onOpenStockAlerts,
            onOpenTeam = onOpenTeam,
            onOpenObd = onOpenObd,
            onOpenMyAttendance = onOpenMyAttendance,
            onOpenMyTasks = onOpenMyTasks,
            onOpenJustification = viewModel::onOpenJustification,
            onDismissJustification = viewModel::onDismissJustification,
            onReviewJustification = viewModel::onReviewJustification
        )
    )
}

private data class DashboardActions(
    val onRefresh: () -> Unit = {},
    val onSelectBranch: () -> Unit = {},
    val onOpenNotifications: () -> Unit = {},
    val onOpenSyncQueue: () -> Unit = {},
    val onOpenProfile: () -> Unit = {},
    val onReceiveVehicle: () -> Unit = {},
    val onRegisterWalkIn: () -> Unit = {},
    val onOpenOrder: (String) -> Unit = {},
    val onOpenOrders: () -> Unit = {},
    val onOpenBays: () -> Unit = {},
    val onOpenStockAlerts: () -> Unit = {},
    val onOpenTeam: () -> Unit = {},
    val onOpenObd: () -> Unit = {},
    val onOpenMyAttendance: () -> Unit = {},
    val onOpenMyTasks: () -> Unit = {},
    val onOpenJustification: (TeamAttendance) -> Unit = {},
    val onDismissJustification: () -> Unit = {},
    val onReviewJustification: (Boolean) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardContent(state: AdminDashboardUiState, actions: DashboardActions) {
    // La decisión se confirma en la mano además de en la fila.
    val haptics = rememberAtelierHaptics()
    val showSkeleton = rememberDelayedLoading(state.showSkeleton)
    LaunchedEffect(state.resolved.size) {
        if (state.resolved.isNotEmpty()) haptics.confirm()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AtelierMainTopBar(
            title = state.tenantName.ifBlank { stringResource(R.string.dashboard_title) },
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
                // La barra flotante tapa el final de la lista: se suma su alto.
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "hero-panel") { DashboardReportPanel(state, actions, loading = showSkeleton) }
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
                val error = state.error
                if (error != null && state.orders.isNotEmpty()) {
                    item(key = "error") {
                        AtelierBanner(
                            message = error.asString(),
                            icon = Icons.Outlined.ErrorOutline,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                item(key = "branch") { BranchCard(state, actions.onSelectBranch) }
                if (state.canClockPersonalAttendance || state.canWorkOwnTasks) {
                    item(key = "personal-shift") { PersonalShiftCard(state, actions) }
                }
                if (state.canCreateWorkOrder) {
                    item(key = "reception") { ReceptionCard(state, actions) }
                }
                item(key = "kpis") { KpiGrid(state, actions, loading = showSkeleton) }
                decisions(state, actions.onOpenOrder, actions.onOpenJustification)
                item(key = "orders-header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.today_open_orders),
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                        action = if (state.orders.size > ORDERS_PREVIEW) {
                            {
                                AtelierButton(
                                    text = stringResource(R.string.today_see_all),
                                    onClick = actions.onOpenOrders,
                                    style = AtelierButtonStyle.Text
                                )
                            }
                        } else null
                    )
                }
                when {
                    !state.canReadWorkOrders -> item(key = "orders-no-permission") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.today_orders_no_permission),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    showSkeleton -> items(3, key = { "bone-$it" }) {
                        AtelierListRow(title = "Orden", subtitle = "Cliente", plate = "ABC123", loading = true)
                    }
                    state.showSkeleton -> Unit
                    state.orders.isEmpty() && error != null && !state.isOnline -> item(key = "orders-offline") {
                        AtelierOfflineState(
                            onRetry = actions.onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.orders.isEmpty() && error != null -> item(key = "orders-error") {
                        AtelierErrorState(
                            message = error.asString(),
                            onRetry = actions.onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.orders.isEmpty() -> item(key = "orders-empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.today_orders_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(state.orders.take(ORDERS_PREVIEW), key = { it.id }) { order ->
                        OrderRow(order, onClick = { actions.onOpenOrder(order.id) })
                    }
                }
                item(key = "floating-bar-space") { Spacer(Modifier.floatingBarPadding()) }
            }
        }
    }

    state.reviewing?.let { item ->
        JustificationSheet(
            item = item,
            state = state,
            onDismiss = actions.onDismissJustification,
            onReview = actions.onReviewJustification
        )
    }
}

/** Resumen vivo de la sede en formato Parte Hero Navy. */
@Composable
private fun DashboardReportPanel(
    state: AdminDashboardUiState,
    actions: DashboardActions,
    loading: Boolean = state.showSkeleton
) {
    val dateText = if (state.today != LocalDate.MIN) ContextDateFormatter.format(state.today) else ""
    val contextText = listOfNotNull(
        state.branchName?.takeIf { it.isNotBlank() },
        dateText.takeIf { it.isNotBlank() }
    ).joinToString(" · ").ifBlank { stringResource(R.string.dashboard_title) }

    val legend = when {
        !state.canReadWorkOrders -> "–"
        !state.showSkeleton && state.carsInWorkshop == 0 -> stringResource(R.string.today_cars_empty)
        else -> pluralStringResource(R.plurals.today_cars, state.carsInWorkshop, state.carsInWorkshop)
    }
    val caption = if (state.canReadWorkOrders) {
        stringResource(R.string.today_caption)
    } else {
        state.branchName ?: stringResource(R.string.dashboard_title)
    }
    val figures = buildList {
        if (state.canReadBays) {
            add(PanelFigure(state.occupiedBaysCount.toString(), stringResource(R.string.today_figure_in_bay), actions.onOpenBays))
        }
        if (state.canReadWorkOrders) {
            add(PanelFigure(state.readyOrdersCount.toString(), stringResource(R.string.today_figure_ready_short), actions.onOpenOrders))
        }
        if (state.canAuditAttendance) {
            add(PanelFigure(state.onShiftCount.toString(), stringResource(R.string.today_figure_on_shift), actions.onOpenTeam))
        }
    }

    AtelierReportPanel(
        context = contextText,
        legend = legend,
        caption = caption,
        figures = figures,
        loading = loading
    )
}

/** Sede activa de la jornada; con más de una sede, toca para cambiarla. */
@Composable
private fun BranchCard(state: AdminDashboardUiState, onSelectBranch: () -> Unit) {
    val colors = AtelierTheme.colors
    AtelierSurface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        onClick = if (state.canChangeBranch) onSelectBranch else null,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceRecessed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = state.branchName ?: stringResource(R.string.dashboard_select_branch_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.dashboard_branch_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.occupiedBaysCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AtelierLiveBeacon(color = colors.okMark, size = 8.dp)
                    Text(
                        text = stringResource(R.string.today_live_bay_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.ok
                    )
                }
            }
            if (state.canChangeBranch) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.inkSecondary
                )
            }
        }
    }
}

/** Accesos personales en "Hoy" para roles operativos que también marcan turno o ejecutan tareas (Jefe / Asesor). */
@Composable
private fun PersonalShiftCard(state: AdminDashboardUiState, actions: DashboardActions) {
    AtelierGroupedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (state.canClockPersonalAttendance) {
            val (subtitleRes, badgeRes) = when (state.personalTodayAttendance) {
                is TodayAttendance.ClockedIn -> R.string.dashboard_my_shift_clocked_in to R.string.floor_hub_shift_active
                is TodayAttendance.ClockInQueued, is TodayAttendance.ClockOutQueued ->
                    R.string.dashboard_my_shift_queued to R.string.floor_hub_shift_pending
                is TodayAttendance.Completed -> R.string.dashboard_my_shift_completed to R.string.floor_hub_shift_closed
                TodayAttendance.NotClockedIn -> R.string.dashboard_my_shift_not_clocked_in to R.string.floor_hub_shift_idle
            }
            AtelierGroupedRow(
                title = stringResource(R.string.dashboard_my_shift_title),
                subtitle = stringResource(subtitleRes),
                icon = Icons.Outlined.Schedule,
                trailingBadgeText = stringResource(badgeRes),
                showDivider = state.canWorkOwnTasks,
                onClick = actions.onOpenMyAttendance
            )
        }
        if (state.canWorkOwnTasks) {
            AtelierGroupedRow(
                title = stringResource(R.string.dashboard_my_tasks_title),
                subtitle = stringResource(R.string.dashboard_my_tasks_sub, state.activePersonalTasksCount),
                icon = Icons.Outlined.AssignmentTurnedIn,
                trailingBadgeText = if (state.activePersonalTasksCount > 0) state.activePersonalTasksCount.toString() else null,
                onClick = actions.onOpenMyTasks
            )
        }
    }
}

@Composable
private fun ReceptionCard(state: AdminDashboardUiState, actions: DashboardActions) {
    val colors = AtelierTheme.colors
    AtelierSurface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.infoContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.dashboard_reception_cta_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (state.occupiedBaysCount > 0) {
                    AtelierLiveBeacon(color = colors.okMark, size = 8.dp)
                }
            }
            Text(
                text = stringResource(R.string.dashboard_reception_cta_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AtelierButton(
                    text = stringResource(R.string.dashboard_search_plate_btn),
                    onClick = actions.onReceiveVehicle,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.DirectionsCar
                )
                if (state.canRegisterCustomers) {
                    AtelierButton(
                        text = stringResource(R.string.dashboard_walk_in_btn),
                        onClick = actions.onRegisterWalkIn,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Tonal,
                        icon = Icons.Outlined.PersonAdd
                    )
                }
            }
        }
    }
}

/** Seis indicadores de la sede, dos por fila. Cada uno aparece solo con el permiso de su fuente. */
@Composable
private fun KpiGrid(
    state: AdminDashboardUiState,
    actions: DashboardActions,
    loading: Boolean = state.showSkeleton
) {
    val cards = buildList<@Composable (Modifier) -> Unit> {
        if (state.canReadWorkOrders) add { m ->
            AtelierKpiCard(
                title = stringResource(R.string.dashboard_kpi_orders),
                value = state.orders.size.toString(),
                subtitle = stringResource(R.string.dashboard_kpi_orders_sub, state.draftOrdersCount, state.inProgressCount),
                modifier = m,
                loading = loading,
                chip = if (state.draftOrdersCount > 0) {
                    {
                        AtelierChip(
                            text = stringResource(R.string.dashboard_kpi_drafts_chip, state.draftOrdersCount),
                            kind = ChipKind.Warn
                        )
                    }
                } else null,
                onClick = actions.onOpenOrders
            )
        }
        if (state.canReadBays) add { m ->
            AtelierKpiCard(
                title = stringResource(R.string.dashboard_kpi_bays),
                value = "${state.occupiedBaysCount}/${state.bays.size}",
                subtitle = stringResource(R.string.dashboard_kpi_bays_sub, state.availableBaysCount),
                modifier = m,
                loading = loading,
                onClick = actions.onOpenBays
            )
        }
        if (state.canReadCustomers) add { m ->
            AtelierKpiCard(
                title = stringResource(R.string.dashboard_kpi_crm),
                value = "${state.customersCount} · ${state.vehiclesCount}",
                subtitle = stringResource(R.string.dashboard_kpi_crm_sub, state.appointments.size),
                modifier = m,
                loading = loading,
                onClick = actions.onReceiveVehicle
            )
        }
        if (state.canReadInventory) add { m ->
            AtelierKpiCard(
                title = stringResource(R.string.dashboard_kpi_stock),
                value = state.stockAlerts.size.toString(),
                subtitle = stringResource(R.string.dashboard_kpi_stock_sub),
                modifier = m,
                loading = loading,
                chip = if (state.stockAlerts.isNotEmpty()) {
                    {
                        AtelierChip(
                            text = stringResource(R.string.notifications_low_stock_chip),
                            kind = ChipKind.Bad
                        )
                    }
                } else null,
                onClick = actions.onOpenStockAlerts
            )
        }
        if (state.canAuditAttendance) add { m ->
            AtelierKpiCard(
                title = stringResource(R.string.dashboard_kpi_attendance),
                value = state.branchAttendances.size.toString(),
                subtitle = stringResource(R.string.dashboard_kpi_attendance_sub, state.pendingJustificationsCount),
                modifier = m,
                loading = loading,
                onClick = actions.onOpenTeam
            )
        }
        if (state.canReadObd) add { m ->
            AtelierKpiCard(
                title = stringResource(R.string.dashboard_kpi_obd),
                value = "${state.activeObdCount}/${state.obdDevices.size}",
                subtitle = stringResource(R.string.dashboard_kpi_obd_sub),
                modifier = m,
                loading = loading,
                onClick = actions.onOpenObd
            )
        }
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { card -> card(Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** "Esperan tu decisión": órdenes sin iniciar, suspensiones por pieza defectuosa y descargos por revisar. Sin nada pendiente, la sección no existe. */
private fun androidx.compose.foundation.lazy.LazyListScope.decisions(
    state: AdminDashboardUiState,
    onOpenOrder: (String) -> Unit,
    onOpenJustification: (TeamAttendance) -> Unit
) {
    val pendingSuspensions = state.visiblePendingSuspensions
    if (state.draftOrders.isEmpty() && pendingSuspensions.isEmpty() && state.justifications.isEmpty() && state.failedJustificationDays == 0) return
    item(key = "decisions-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.today_decisions),
            count = state.decisionsCount,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    items(pendingSuspensions, key = { "susp-${it.taskId}" }) { item ->
        val sub = listOfNotNull(
            item.serviceName ?: item.taskDescription.takeIf { it.isNotBlank() },
            item.reason?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        AtelierDecisionRow(
            title = stringResource(R.string.today_decision_suspension),
            subtitle = sub.ifBlank { null },
            plate = item.plate,
            onClick = { onOpenOrder(item.workOrderId) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(state.draftOrders, key = { "draft-${it.id}" }) { order ->
        AtelierDecisionRow(
            title = stringResource(R.string.today_decision_draft),
            subtitle = order.customerName ?: order.numberLabel(),
            plate = order.plate,
            amount = order.totalAmount?.takeIf { it.amount.signum() > 0 }?.format(),
            onClick = { onOpenOrder(order.id) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(state.justifications, key = { "just-${it.id}" }) { item ->
        val approved = state.resolved[item.id]
        AtelierDecisionRow(
            title = stringResource(item.status.justificationTitleRes()),
            subtitle = item.whoAndWhen(),
            resolved = approved?.let {
                stringResource(if (it) R.string.today_justification_approved else R.string.today_justification_rejected)
            },
            onClick = { onOpenJustification(item) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    if (state.failedJustificationDays > 0) {
        item(key = "decisions-partial") {
            AtelierBanner(
                message = pluralStringResource(
                    R.plurals.today_justifications_partial, state.failedJustificationDays, state.failedJustificationDays
                ),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Wait,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun OrderRow(order: WorkOrder, onClick: () -> Unit) {
    AtelierListRow(
        title = order.customerName ?: order.numberLabel(),
        subtitle = listOfNotNull(order.numberLabel().takeIf { order.customerName != null }, order.bayName)
            .joinToString(" · ")
            .ifEmpty { null },
        plate = order.plate,
        onClick = onClick,
        trailing = { AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind()) }
    )
}

@Composable
private fun JustificationSheet(
    item: TeamAttendance,
    state: AdminDashboardUiState,
    onDismiss: () -> Unit,
    onReview: (Boolean) -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(item.status.justificationTitleRes()),
        onDismiss = onDismiss,
        actions = {
            when {
                !state.canApproveJustifications ->
                    AtelierNoPermissionState(stringResource(R.string.today_justification_no_permission))
                !state.isOnline -> AtelierRequiresConnection()
                else -> {
                    AtelierButton(
                        text = stringResource(R.string.today_justification_approve),
                        onClick = { onReview(true) },
                        loading = state.isSubmittingReview
                    )
                    AtelierButton(
                        text = stringResource(R.string.today_justification_reject),
                        onClick = { onReview(false) },
                        style = AtelierButtonStyle.Destructive,
                        enabled = !state.isSubmittingReview
                    )
                }
            }
        }
    ) {
        Text(
            text = item.whoAndWhen(),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = item.justificationReason?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.today_justification_no_reason),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        state.reviewError?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

@Composable
private fun WorkOrder.numberLabel(): String =
    internalNumber?.let { stringResource(R.string.today_order_number, it) } ?: stringResource(R.string.today_order_no_number)

/** El backend no envía el nombre del colaborador (H36): se identifica por su cargo y la fecha de la jornada. */
@Composable
private fun TeamAttendance.whoAndWhen(): String =
    listOfNotNull(jobTitle ?: stringResource(R.string.today_collaborator_fallback), clockIn?.formatLimaDate())
        .joinToString(" · ")

private fun AttendanceStatus?.justificationTitleRes(): Int = when (this) {
    AttendanceStatus.LATE -> R.string.today_decision_justification_late
    AttendanceStatus.ABSENT -> R.string.today_decision_justification_absent
    else -> R.string.today_decision_justification
}

// region Previews

private fun previewOrder(id: String, number: Int, plate: String, customer: String, status: WorkOrderStatus, bay: String?, total: String) =
    WorkOrder(
        id = id, internalNumber = number, branchId = "b1", vehicleId = "v$id", customerId = "c$id",
        currentBayId = bay?.let { "bay" }, bayName = bay, status = status, diagnosticSummary = null, mileageIn = null,
        subtotal = null, taxAmount = null, totalAmount = Money.of(total, Money.PEN), plate = plate, customerName = customer
    )

private fun previewJustification() = TeamAttendance(
    id = "a1", membershipId = "m1", branchId = "b1", shiftId = "s1",
    clockIn = Instant.parse("2026-11-16T13:42:00Z"), clockOut = null, status = AttendanceStatus.LATE,
    distanceToBranchMeters = 12.0, workedMinutes = null, overtimeMinutes = null,
    justificationReason = "Paro de transporte en la Panamericana Sur; adjunté la constancia.",
    justificationStatus = JustificationStatus.PENDING, reviewedBy = null, reviewedAt = null, jobTitle = "Mecánico"
)

private val previewState = AdminDashboardUiState(
    today = LocalDate.of(2026, 11, 16),
    branchName = "Sede Surco",
    canChangeBranch = true,
    orders = listOf(
        previewOrder("1", 1042, "BKR418", "Rosa Quispe Mamani", WorkOrderStatus.DRAFT, null, "480.00"),
        previewOrder("2", 1041, "F5U731", "Transportes Huamán S.A.C.", WorkOrderStatus.IN_PROGRESS, "Bahía 2", "1260.50"),
        previewOrder("3", 1040, "AXT209", "Luis Alberto Paredes", WorkOrderStatus.IN_PROGRESS, "Bahía 1", "320.00"),
        previewOrder("4", 1038, "D2C604", "Carmen Flores", WorkOrderStatus.COMPLETED, null, "890.00")
    ),
    bays = listOf(
        BayBoardItem(
            WorkBay("bay1", "b1", "Bahía 1", BayType.LIFT, BayStatus.OCCUPIED, "3"),
            BayOccupant("3", 1040, "AXT209", listOf("m1"), Duration.ofMinutes(45))
        ),
        BayBoardItem(
            WorkBay("bay2", "b1", "Bahía 2", BayType.LIFT, BayStatus.OCCUPIED, "2"),
            BayOccupant("2", 1041, "F5U731", listOf("m2"), Duration.ofMinutes(80))
        ),
        BayBoardItem(
            WorkBay("bay3", "b1", "Bahía 3", BayType.DIAGNOSTIC_PIT, BayStatus.AVAILABLE, null),
            null
        )
    ),
    justifications = listOf(previewJustification()),
    hasLoaded = true,
    pendingSyncCount = 2,
    unreadNotificationsCount = 3,
    canCreateWorkOrder = true,
    canReadWorkOrders = true,
    canStartWorkOrders = true,
    canReadBays = true,
    canAuditAttendance = true,
    canApproveJustifications = true
)

@Composable
private fun PreviewContent(state: AdminDashboardUiState) {
    AdminDashboardContent(state, DashboardActions())
}

@Preview(name = "Hoy · claro", widthDp = 390, heightDp = 844)
@Composable
private fun AdminDashboardPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Hoy · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminDashboardPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Hoy · cargando sin caché", widthDp = 390, heightDp = 844)
@Composable
private fun AdminDashboardPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(AdminDashboardUiState(today = BusinessTime.limaDate(Instant.EPOCH), canReadWorkOrders = true, canReadBays = true))
}

// endregion
