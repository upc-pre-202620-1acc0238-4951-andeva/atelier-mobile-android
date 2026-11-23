package com.andeva.atelier.mobile.features.main.presentation.notifications

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.notifications.NotificationRecord
import com.andeva.atelier.mobile.core.notifications.PushChannel
import com.andeva.atelier.mobile.core.notifications.PushDestination
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

private val NotificationTimeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)

/** Cuántas alertas de cada tipo se listan antes de mandar a su pantalla. */
private const val ALERTS_PREVIEW = 5

/**
 * Bandeja: arriba lo que pide atención (cola sin sincronizar, stock bajo, descargos, órdenes sin iniciar) y debajo
 * los avisos push de la sesión. Cada fila lleva a su destino. Lo no leído lleva el chip "Nuevo", no solo énfasis.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenOrder: (workOrderId: String) -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenAttendanceReview: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    NotificationsContent(
        state = state,
        notificationsEnabled = notificationsEnabled,
        onEnableNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        },
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onMarkAllRead = viewModel::onMarkAllRead,
        onOpenPushRecord = viewModel::onOpenPushRecord,
        onOpenOrder = onOpenOrder,
        onOpenStockAlerts = onOpenStockAlerts,
        onOpenAttendanceReview = onOpenAttendanceReview,
        onOpenSyncQueue = onOpenSyncQueue,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    state: NotificationsUiState,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenPushRecord: (NotificationRecord) -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenAttendanceReview: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noCache = state.isLoading && state.pushHistory.isEmpty() && state.totalActiveAlerts == 0
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.notif_title), onBack = onBack) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && !noCache,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (!notificationsEnabled) {
                    item(key = "disabled") {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                        ) {
                            AtelierBanner(
                                title = stringResource(R.string.notifications_disabled_title),
                                message = stringResource(R.string.notifications_disabled_body),
                                icon = Icons.Outlined.Notifications,
                                kind = BannerKind.Wait
                            )
                            AtelierButton(
                                text = stringResource(R.string.notifications_enable_btn),
                                onClick = onEnableNotifications,
                                modifier = Modifier.fillMaxWidth(),
                                style = AtelierButtonStyle.Tonal
                            )
                        }
                    }
                }
                attention(state, onOpenOrder, onOpenStockAlerts, onOpenAttendanceReview, onOpenSyncQueue)
                item(key = "push-header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.notif_section_received),
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp),
                        action = if (state.unreadPushCount > 0) {
                            {
                                AtelierButton(
                                    text = stringResource(R.string.notifications_mark_all_read),
                                    onClick = onMarkAllRead,
                                    style = AtelierButtonStyle.Text
                                )
                            }
                        } else null
                    )
                }
                when {
                    noCache -> items(3, key = { "bone-$it" }) {
                        AtelierListRow(title = "Aviso", subtitle = "Detalle del aviso", loading = true)
                    }
                    state.pushHistory.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.notif_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(state.pushHistory, key = { it.id }) { record ->
                        PushRecordRow(record = record, onClick = { onOpenPushRecord(record) })
                    }
                }
            }
        }
    }
}

/** "Piden tu atención": cada tipo con su conteo y su destino. Sin nada pendiente, la sección no existe. */
private fun androidx.compose.foundation.lazy.LazyListScope.attention(
    state: NotificationsUiState,
    onOpenOrder: (String) -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenAttendanceReview: () -> Unit,
    onOpenSyncQueue: () -> Unit
) {
    if (state.totalActiveAlerts == 0) return
    item(key = "attention-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.notif_section_attention),
            count = state.totalActiveAlerts,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
    }
    if (state.pendingSyncCount > 0) {
        item(key = "sync") {
            AtelierListRow(
                title = pluralStringResource(R.plurals.notif_sync_pending, state.pendingSyncCount, state.pendingSyncCount),
                subtitle = stringResource(R.string.notif_sync_pending_body),
                onClick = onOpenSyncQueue,
                trailing = { AtelierChip(stringResource(R.string.sync_op_pending), kind = ChipKind.Idle) }
            )
        }
    }
    items(state.stockAlerts.take(ALERTS_PREVIEW), key = { "stock-${it.itemId}" }) { alert ->
        AtelierListRow(
            title = alert.name,
            subtitle = stringResource(
                R.string.notifications_stock_deficit_line,
                alert.sku,
                alert.currentStock.formatQuantity(),
                alert.minimumStock.formatQuantity()
            ),
            onClick = onOpenStockAlerts,
            trailing = { AtelierChip(stringResource(R.string.notifications_low_stock_chip), kind = ChipKind.Warn) }
        )
    }
    items(state.pendingJustifications, key = { "just-${it.attendance.id.value}" }) { item ->
        AtelierListRow(
            title = item.collaboratorLabel,
            subtitle = item.attendance.justificationReason?.takeIf { it.isNotBlank() },
            onClick = onOpenAttendanceReview,
            trailing = { AtelierChip(stringResource(R.string.notifications_justification_chip), kind = ChipKind.Wait) }
        )
    }
    items(state.draftOrdersWaitingApproval.take(ALERTS_PREVIEW), key = { "draft-${it.id}" }) { order ->
        AtelierListRow(
            title = order.customerName ?: order.internalNumber?.let { stringResource(R.string.today_order_number, it) }
                ?: stringResource(R.string.today_order_no_number),
            subtitle = stringResource(R.string.today_decision_draft),
            plate = order.plate,
            onClick = { onOpenOrder(order.id) },
            trailing = { AtelierChip(stringResource(R.string.notif_draft_chip), kind = ChipKind.Wait) }
        )
    }
}

@Composable
private fun PushRecordRow(record: NotificationRecord, onClick: () -> Unit) {
    AtelierListRow(
        title = record.title,
        subtitle = listOf(record.body, NotificationTimeFormat.format(record.receivedAt))
            .filter { it.isNotBlank() }
            .joinToString(" · "),
        onClick = onClick,
        trailing = if (!record.read) {
            { AtelierChip(stringResource(R.string.notif_unread), kind = ChipKind.Info) }
        } else null
    )
}

// region Previews

private val previewState = NotificationsUiState(
    pushHistory = listOf(
        NotificationRecord(
            id = "n1", title = "Propuesta pendiente", body = "Andrés V. encontró pastillas de freno gastadas en BKR418.",
            destination = PushDestination.WorkOrderDetail("o1"), channel = PushChannel.TASKS,
            receivedAt = Instant.parse("2026-11-16T19:32:00Z"), read = false
        ),
        NotificationRecord(
            id = "n2", title = "Stock bajo", body = "Filtro de aceite Mann W712 llegó a su mínimo.",
            destination = null, channel = PushChannel.INVENTORY,
            receivedAt = Instant.parse("2026-11-16T15:05:00Z"), read = true
        )
    ),
    stockAlerts = listOf(
        StockAlert(
            itemId = "i1", name = "Filtro de aceite Mann W712", sku = "FLT-0712", category = null,
            currentStock = BigDecimal("2"), minimumStock = BigDecimal("6"), deficit = BigDecimal("4")
        )
    ),
    draftOrdersWaitingApproval = listOf(
        WorkOrder(
            id = "o9", internalNumber = 1042, branchId = "b1", vehicleId = "v1", customerId = "c1", currentBayId = null,
            bayName = null, status = WorkOrderStatus.DRAFT, diagnosticSummary = null, mileageIn = null, subtotal = null,
            taxAmount = null, totalAmount = null, plate = "BKR418", customerName = "Rosa Quispe Mamani"
        )
    ),
    pendingSyncCount = 2,
    isLoading = false
)

@Composable
private fun PreviewContent(state: NotificationsUiState, enabled: Boolean = true) {
    NotificationsContent(state, enabled, {}, {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Notificaciones · claro", widthDp = 390, heightDp = 844)
@Composable
private fun NotificationsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Notificaciones · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState, enabled = false) }

@Preview(name = "Notificaciones · vacío", widthDp = 390, heightDp = 844)
@Composable
private fun NotificationsPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(NotificationsUiState(isLoading = false))
}

// endregion
