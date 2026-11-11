package com.andeva.atelier.mobile.features.main.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes

/** Panel operativo de sede: inicio para perfiles administrativos (Tarea 2). */
@Composable
fun AdminDashboardScreen(
    onSelectBranch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenReception: () -> Unit,
    onOpenWalkInRegister: () -> Unit,
    onOpenOrdersList: () -> Unit,
    onOpenOrderDetail: (workOrderId: String) -> Unit,
    onOpenBays: () -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenAttendanceHub: () -> Unit,
    onOpenObdInventory: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMoreHub: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminDashboardContent(
        state = state,
        onSelectBranch = onSelectBranch,
        onOpenNotifications = onOpenNotifications,
        onOpenSyncQueue = onOpenSyncQueue,
        onOpenReception = onOpenReception,
        onOpenWalkInRegister = onOpenWalkInRegister,
        onOpenOrdersList = onOpenOrdersList,
        onOpenOrderDetail = onOpenOrderDetail,
        onOpenBays = onOpenBays,
        onOpenStockAlerts = onOpenStockAlerts,
        onOpenAttendanceHub = onOpenAttendanceHub,
        onOpenObdInventory = onOpenObdInventory,
        onOpenMoreHub = onOpenMoreHub,
        onRefresh = viewModel::refresh,
        modifier = modifier
    )
}

@Composable
internal fun AdminDashboardContent(
    state: AdminDashboardUiState,
    onSelectBranch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenReception: () -> Unit,
    onOpenWalkInRegister: () -> Unit,
    onOpenOrdersList: () -> Unit,
    onOpenOrderDetail: (workOrderId: String) -> Unit,
    onOpenBays: () -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenAttendanceHub: () -> Unit,
    onOpenObdInventory: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMoreHub: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardHeader(
            state = state,
            onOpenMoreHub = onOpenMoreHub,
            onOpenSyncQueue = onOpenSyncQueue,
            onOpenNotifications = onOpenNotifications
        )

        // El resto del contenido conserva 16 dp a ambos lados; el encabezado usa 8 dp a la derecha para los iconos.
        Column(
            modifier = Modifier.padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BranchSelectorCard(branchName = state.branchName, onClick = onSelectBranch)

            ReceptionHeroCard(onOpenReception = onOpenReception, onOpenWalkInRegister = onOpenWalkInRegister)

            KpiRow {
                KpiCard(
                    title = stringResource(R.string.dashboard_kpi_orders),
                    mainValue = "${state.orders.size}",
                    subtitle = stringResource(
                        R.string.dashboard_kpi_orders_sub,
                        state.draftOrders.size,
                        state.inProgressOrders.size
                    ),
                    chipText = state.draftOrders.size.takeIf { it > 0 }
                        ?.let { stringResource(R.string.dashboard_kpi_drafts_chip, it) },
                    chipKind = ChipKind.Wait,
                    onClick = onOpenOrdersList,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KpiCard(
                    title = stringResource(R.string.dashboard_kpi_bays),
                    mainValue = "${state.occupiedBaysCount}/${state.bays.size}",
                    subtitle = stringResource(R.string.dashboard_kpi_bays_sub, state.availableBaysCount),
                    chipText = stringResource(R.string.bay_status_available).takeIf { state.availableBaysCount > 0 },
                    chipKind = ChipKind.Ok,
                    onClick = onOpenBays,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            KpiRow {
                KpiCard(
                    title = stringResource(R.string.dashboard_kpi_crm),
                    mainValue = "${state.customersCount} · ${state.vehiclesCount}",
                    subtitle = stringResource(R.string.dashboard_kpi_crm_sub, state.appointments.size),
                    chipText = null,
                    chipKind = ChipKind.Info,
                    onClick = onOpenReception,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KpiCard(
                    title = stringResource(R.string.dashboard_kpi_stock),
                    mainValue = "${state.stockAlerts.size}",
                    subtitle = stringResource(R.string.dashboard_kpi_stock_sub),
                    chipText = stringResource(R.string.notifications_low_stock_chip).takeIf { state.stockAlerts.isNotEmpty() },
                    chipKind = ChipKind.Bad,
                    onClick = onOpenStockAlerts,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            if (state.canAuditAttendance || state.canReadObd) {
                KpiRow {
                    KpiCard(
                        title = stringResource(R.string.dashboard_kpi_attendance),
                        mainValue = "${state.branchAttendances.size}",
                        subtitle = stringResource(R.string.dashboard_kpi_attendance_sub, state.pendingJustificationsCount),
                        chipText = stringResource(R.string.notifications_justification_chip)
                            .takeIf { state.pendingJustificationsCount > 0 },
                        chipKind = ChipKind.Wait,
                        onClick = onOpenAttendanceHub,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    KpiCard(
                        title = stringResource(R.string.dashboard_kpi_obd),
                        mainValue = "${state.activeObdCount}/${state.obdDevices.size}",
                        subtitle = stringResource(R.string.dashboard_kpi_obd_sub),
                        chipText = null,
                        chipKind = ChipKind.Info,
                        onClick = onOpenObdInventory,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }

            // Proformas en borrador pendientes de tareas, PDF o aceptación del cliente (Tareas 4 y 5)
            if (state.draftOrders.isNotEmpty()) {
                SectionHeader(
                    title = stringResource(R.string.dashboard_section_drafts, state.draftOrders.size),
                    actionText = stringResource(R.string.dashboard_see_all),
                    onAction = onOpenOrdersList
                )
                state.draftOrders.take(5).forEach { order ->
                    DashboardOrderCard(
                        order = order,
                        ctaLabel = stringResource(R.string.dashboard_open_proforma_btn),
                        onClick = { onOpenOrderDetail(order.id) }
                    )
                }
            }

            // Órdenes en intervención técnica en bahía (Tarea 5)
            if (state.inProgressOrders.isNotEmpty()) {
                SectionHeader(
                    title = stringResource(R.string.dashboard_section_in_progress, state.inProgressOrders.size),
                    actionText = stringResource(R.string.dashboard_see_bays),
                    onAction = onOpenBays
                )
                state.inProgressOrders.take(5).forEach { order ->
                    DashboardOrderCard(
                        order = order,
                        ctaLabel = stringResource(R.string.dashboard_manage_order_btn),
                        onClick = { onOpenOrderDetail(order.id) }
                    )
                }
            }

            AtelierButton(
                text = stringResource(R.string.bays_refresh),
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                loading = state.isRefreshing
            )
        }
    }
}

/** Nombre del taller y usuario a la izquierda; cola offline y bandeja de notificaciones como iconos a la derecha. */
@Composable
private fun DashboardHeader(
    state: AdminDashboardUiState,
    onOpenMoreHub: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMoreHub, modifier = Modifier.padding(end = 4.dp)) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.more_hub_title),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.tenantName.ifBlank { stringResource(R.string.dashboard_title) },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.fullName.isNotBlank()) {
                Text(
                    text = state.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (state.pendingSyncCount > 0) {
            HeaderIconButton(
                icon = Icons.Outlined.Sync,
                contentDescription = stringResource(R.string.sync_queue_title),
                badgeCount = state.pendingSyncCount,
                onClick = onOpenSyncQueue
            )
        }
        HeaderIconButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = stringResource(R.string.notifications_title),
            badgeCount = state.totalBadgeAlerts,
            onClick = onOpenNotifications
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) Badge { Text(badgeCount.coerceAtMost(99).toString()) }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun BranchSelectorCard(branchName: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Storefront,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.dashboard_branch_label),
                style = MaterialTheme.typography.labelMedium,
                color = AtelierTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = branchName ?: stringResource(R.string.dashboard_select_branch_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = stringResource(R.string.dashboard_change_branch_btn),
            tint = AtelierTheme.colors.muted
        )
    }
}

@Composable
private fun ReceptionHeroCard(onOpenReception: () -> Unit, onOpenWalkInRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_reception_cta_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.dashboard_reception_cta_body),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        Spacer(Modifier.height(4.dp))
        AtelierButton(
            text = stringResource(R.string.dashboard_search_plate_btn),
            onClick = onOpenReception,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.DirectionsCar
        )
        AtelierButton(
            text = stringResource(R.string.dashboard_walk_in_btn),
            onClick = onOpenWalkInRegister,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal,
            icon = Icons.Outlined.PersonAdd
        )
    }
}

/** Fila de dos KPIs con la misma altura aunque un subtítulo ocupe más líneas. */
@Composable
private fun KpiRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    content = content
)

@Composable
private fun KpiCard(
    title: String,
    mainValue: String,
    subtitle: String,
    chipText: String?,
    chipKind: ChipKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = AtelierTheme.colors.inkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (chipText != null) {
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                AtelierChip(text = chipText, kind = chipKind)
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AtelierTheme.colors.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier
                .clip(CardShape)
                .clickable(onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun DashboardOrderCard(
    order: WorkOrder,
    ctaLabel: String,
    onClick: () -> Unit
) {
    val code = order.internalNumber?.let { stringResource(R.string.order_code_format, it) }
        ?: order.id.take(8)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = listOfNotNull(code, order.plate).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            order.status?.let { status ->
                AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind())
            }
        }
        val subtitle = listOfNotNull(
            order.customerName,
            order.bayName,
            order.mileageIn?.let { "$it km" }
        ).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        AtelierButton(
            text = ctaLabel,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal
        )
    }
}

private val previewOrders = listOf(
    WorkOrder(
        "wo2", 2, "b1", "v2", "c2", null, null, WorkOrderStatus.DRAFT, null, 12_000,
        null, null, null, customerName = "Prueba Atelier QA"
    ),
    WorkOrder(
        "wo1", 1, "b1", "v1", "c1", "bay1", "Elevador 1", WorkOrderStatus.IN_PROGRESS, null, 96_540,
        null, null, null, plate = "D4K719", customerName = "Transportes Andinos S.A.C."
    )
)

@Preview(name = "Panel de sede · claro", widthDp = 360, heightDp = 1600, showBackground = true)
@Composable
private fun AdminDashboardPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AdminDashboardContent(
        state = AdminDashboardUiState(
            fullName = "Pedro Suárez",
            tenantName = "Taller Suárez Automotriz",
            orders = previewOrders,
            customersCount = 5,
            vehiclesCount = 3,
            pendingSyncCount = 2,
            unreadNotificationsCount = 2,
            canAuditAttendance = true,
            canReadObd = true
        ),
        onSelectBranch = {}, onOpenNotifications = {}, onOpenSyncQueue = {}, onOpenReception = {},
        onOpenWalkInRegister = {}, onOpenOrdersList = {}, onOpenOrderDetail = {}, onOpenBays = {},
        onOpenStockAlerts = {}, onOpenAttendanceHub = {}, onOpenObdInventory = {}, onRefresh = {}
    )
}
