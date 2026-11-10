package com.andeva.atelier.mobile.features.workorders.presentation.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.designsystem.theme.PillShape
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)

/** Órdenes de la sede. [onOpenOrder] abre el detalle; [onOpenNewOrder] abre el formulario de ingreso. */
@Composable
fun WorkOrdersScreen(
    onOpenOrder: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenMenu: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenNewOrder: (vehicleId: String, customerId: String, workOrderId: String) -> Unit = { _, _, _ -> },
    onNavigateToReception: () -> Unit = {},
    viewModel: WorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkOrdersContent(
        state = state,
        onBack = onBack,
        onOpenMenu = onOpenMenu,
        onOpenNotifications = onOpenNotifications,
        onOpenSync = onOpenSync,
        onRefresh = viewModel::refresh,
        onOpenOrder = onOpenOrder,
        onOpenNewOrder = onOpenNewOrder,
        onNavigateToReception = onNavigateToReception,
        onOpenNewOrderSheet = viewModel::onOpenNewOrderSheet,
        onDismissNewOrderSheet = viewModel::onDismissNewOrderSheet,
        onSelectAppointmentForArrival = { appt ->
            viewModel.onSelectAppointmentForArrival(appt, onOpenNewOrder)
        },
        modifier = modifier
    )
}

@Composable
internal fun WorkOrdersContent(
    state: WorkOrdersUiState,
    onRefresh: () -> Unit,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenMenu: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenNewOrder: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToReception: () -> Unit = {},
    onOpenNewOrderSheet: () -> Unit = {},
    onDismissNewOrderSheet: () -> Unit = {},
    onSelectAppointmentForArrival: (Appointment) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        if (onBack != null) {
            AtelierTopBar(title = stringResource(R.string.orders_title), onBack = onBack)
        } else if (onOpenMenu != null) {
            AtelierMainTopBar(
                title = stringResource(R.string.orders_title),
                onOpenMenu = onOpenMenu,
                onOpenNotifications = onOpenNotifications,
                onOpenSync = onOpenSync
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.orders_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )

            if (state.canCreate) {
                AtelierButton(
                    text = stringResource(R.string.orders_new_btn),
                    onClick = onOpenNewOrderSheet,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Add
                )
            }

            if (!state.canRead) {
                AtelierBanner(
                    message = stringResource(R.string.orders_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            if (state.showingCache) {
                AtelierBanner(
                    message = state.error?.asString() ?: stringResource(R.string.orders_cache_notice),
                    icon = Icons.Outlined.WifiOff,
                    kind = BannerKind.Wait
                )
            }
            when {
                !state.isLoaded -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                state.orders.isEmpty() -> Text(
                    text = stringResource(R.string.orders_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                else -> state.orders.forEach { order ->
                    OrderCard(
                        order = order,
                        onClick = { onOpenOrder(order.id) },
                        onCompleteIntake = {
                            onOpenNewOrder(order.vehicleId, order.customerId, order.id)
                        }
                    )
                }
            }
            AtelierButton(
                text = stringResource(R.string.tasks_refresh),
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                loading = state.isRefreshing
            )
        }
    }

    if (state.showNewOrderSheet) {
        NewOrderChoiceDialog(
            appointments = state.appointmentsToday,
            arrivingId = state.arrivingAppointmentId,
            onSelectAppointment = onSelectAppointmentForArrival,
            onNavigateToReception = {
                onDismissNewOrderSheet()
                onNavigateToReception()
            },
            onDismiss = onDismissNewOrderSheet
        )
    }
}

@Composable
private fun NewOrderChoiceDialog(
    appointments: List<Appointment>,
    arrivingId: String?,
    onSelectAppointment: (Appointment) -> Unit,
    onNavigateToReception: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text
            )
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.orders_new_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.orders_new_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.orders_new_by_appointment_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (appointments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.orders_new_no_appointments),
                            style = MaterialTheme.typography.bodySmall,
                            color = AtelierTheme.colors.muted,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    appointments.forEach { appt ->
                        val isArriving = arrivingId == appt.id
                        AppointmentOptionCard(
                            appointment = appt,
                            isArriving = isArriving,
                            enabled = arrivingId == null,
                            onClick = { onSelectAppointment(appt) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.orders_new_walk_in_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Surface(
                    onClick = onNavigateToReception,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.orders_new_walk_in_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.orders_new_walk_in_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = AtelierTheme.colors.inkSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AppointmentOptionCard(
    appointment: Appointment,
    isArriving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val timeStr = TimeFormat.format(appointment.scheduledAt)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                appointment.plate?.let { plate ->
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = plate,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (isArriving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            appointment.customerName?.let { customer ->
                Text(
                    text = customer,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            appointment.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AtelierTheme.colors.ok,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.orders_new_appointment_arrive_cta),
                    style = MaterialTheme.typography.labelSmall,
                    color = AtelierTheme.colors.ok
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: WorkOrder,
    onClick: () -> Unit,
    onCompleteIntake: () -> Unit = {}
) {
    val isDraft = order.status == WorkOrderStatus.DRAFT
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind())
            order.plate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = order.internalNumber?.let { stringResource(R.string.task_card_work_order, it) }
                ?: stringResource(R.string.orders_no_number),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        order.customerName?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
        }
        order.totalAmount?.let {
            Text(
                text = stringResource(R.string.orders_total, it.format()),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        if (isDraft) {
            AtelierButton(
                text = stringResource(R.string.orders_complete_draft_cta),
                onClick = onCompleteIntake,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Tonal
            )
        }
    }
}

private fun previewOrder(id: String, number: Int, status: WorkOrderStatus) = WorkOrder(
    id = id, internalNumber = number, branchId = "b1", vehicleId = "v1", customerId = "c1", currentBayId = null,
    bayName = null, status = status, diagnosticSummary = null, mileageIn = null, subtotal = null, taxAmount = null,
    totalAmount = Money(BigDecimal("1250.00"), Money.PEN), plate = "ABC-123", customerName = "Ana Torres"
)

@Preview(name = "Órdenes · oscuro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WorkOrdersPreview() = AtelierWorkshopTheme(darkTheme = true) {
    WorkOrdersContent(
        state = WorkOrdersUiState(
            isLoaded = true,
            canCreate = true,
            orders = listOf(previewOrder("1", 1, WorkOrderStatus.IN_PROGRESS), previewOrder("2", 2, WorkOrderStatus.DRAFT))
        ),
        onBack = {}, onRefresh = {}, onOpenOrder = {}
    )
}
