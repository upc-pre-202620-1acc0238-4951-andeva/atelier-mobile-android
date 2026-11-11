package com.andeva.atelier.mobile.features.main.presentation.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.notifications.NotificationRecord
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import java.time.format.DateTimeFormatter

private val NotificationTimeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)

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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.notifications_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!notificationsEnabled) {
                AtelierBanner(
                    title = stringResource(R.string.notifications_disabled_title),
                    message = stringResource(R.string.notifications_disabled_body),
                    icon = Icons.Outlined.Notifications,
                    kind = BannerKind.Wait
                )
                AtelierButton(
                    text = stringResource(R.string.notifications_enable_btn),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Tonal
                )
            } else {
                AtelierBanner(
                    message = stringResource(R.string.notifications_enabled_banner),
                    icon = Icons.Outlined.CheckCircle,
                    kind = BannerKind.Ok
                )
            }

            if (state.pendingSyncCount > 0) {
                AlertCard(
                    title = stringResource(R.string.notifications_sync_pending_title, state.pendingSyncCount),
                    subtitle = stringResource(R.string.notifications_sync_pending_body),
                    chipText = stringResource(R.string.sync_op_pending),
                    chipKind = ChipKind.Wait,
                    onClick = onOpenSyncQueue
                )
            }

            if (state.stockAlerts.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.notifications_section_stock, state.stockAlerts.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                state.stockAlerts.take(5).forEach { alert ->
                    AlertCard(
                        title = alert.name,
                        subtitle = stringResource(
                            R.string.notifications_stock_deficit_line,
                            alert.sku,
                            alert.currentStock.formatQuantity(),
                            alert.minimumStock.formatQuantity()
                        ),
                        chipText = stringResource(R.string.notifications_low_stock_chip),
                        chipKind = ChipKind.Bad,
                        onClick = onOpenStockAlerts
                    )
                }
            }

            if (state.pendingJustifications.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.notifications_section_justifications, state.pendingJustifications.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                state.pendingJustifications.forEach { item ->
                    AlertCard(
                        title = item.collaboratorLabel,
                        subtitle = item.attendance.justificationReason.orEmpty(),
                        chipText = stringResource(R.string.notifications_justification_chip),
                        chipKind = ChipKind.Wait,
                        onClick = onOpenAttendanceReview
                    )
                }
            }

            if (state.draftOrdersWaitingApproval.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.notifications_section_quotations, state.draftOrdersWaitingApproval.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                state.draftOrdersWaitingApproval.take(5).forEach { order ->
                    val code = order.internalNumber?.let { stringResource(R.string.order_code_format, it) }
                        ?: order.id.take(8)
                    val subtitle = listOfNotNull(order.plate, order.customerName).joinToString(" · ")
                    AlertCard(
                        title = code,
                        subtitle = subtitle,
                        chipText = stringResource(R.string.order_status_waiting_customer),
                        chipKind = ChipKind.Info,
                        onClick = { onOpenOrder(order.id) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.notifications_section_push),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (state.unreadPushCount > 0) {
                    AtelierButton(
                        text = stringResource(R.string.notifications_mark_all_read),
                        onClick = viewModel::onMarkAllRead,
                        style = AtelierButtonStyle.Text
                    )
                }
            }

            if (state.pushHistory.isEmpty() && state.totalActiveAlerts == 0 && !state.isLoading) {
                AtelierBanner(
                    title = stringResource(R.string.notifications_empty_title),
                    message = stringResource(R.string.notifications_empty_body),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
            } else {
                state.pushHistory.forEach { record ->
                    PushRecordCard(
                        record = record,
                        onClick = { viewModel.onOpenPushRecord(record) }
                    )
                }
            }

            AtelierButton(
                text = stringResource(R.string.bays_refresh),
                onClick = viewModel::refresh,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                loading = state.isLoading
            )
        }
    }
}

@Composable
private fun AlertCard(
    title: String,
    subtitle: String,
    chipText: String,
    chipKind: ChipKind,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            AtelierChip(text = chipText, kind = chipKind)
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

@Composable
private fun PushRecordCard(
    record: NotificationRecord,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (!record.read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = NotificationTimeFormat.format(record.receivedAt),
                style = MaterialTheme.typography.bodySmall,
                color = AtelierTheme.colors.muted
            )
        }
        if (record.body.isNotBlank()) {
            Text(
                text = record.body,
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}
