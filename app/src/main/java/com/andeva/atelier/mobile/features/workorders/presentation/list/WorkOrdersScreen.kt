package com.andeva.atelier.mobile.features.workorders.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.time.Duration
import java.time.Instant

/** Filtros rápidos en barra: Todas, Por cobrar (COMPLETED), En curso y Borrador. */
private val FilterOptions: List<WorkOrderStatus?> = listOf(
    null,
    WorkOrderStatus.COMPLETED,
    WorkOrderStatus.IN_PROGRESS,
    WorkOrderStatus.DRAFT
)

private fun WorkOrderStatus?.filterSegmentLabelRes(): Int = when (this) {
    null -> R.string.orders_filter_all
    WorkOrderStatus.COMPLETED -> R.string.orders_filter_to_collect
    else -> labelRes()
}

/**
 * Raíz de la pestaña "Órdenes": las órdenes sin cerrar de la sede, con filtro por estado y búsqueda por placa.
 * "Recibir vehículo" vive en el botón de acción de la barra flotante, no aquí.
 */
@Composable
fun WorkOrdersScreen(
    onOpenOrder: (workOrderId: String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkOrdersContent(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenOrder = onOpenOrder,
        onOpenNotifications = onOpenNotifications,
        onOpenSync = onOpenSync,
        onStatusFilterChange = viewModel::onStatusFilterChange,
        onQueryChange = viewModel::onQueryChange,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkOrdersContent(
    state: WorkOrdersUiState,
    onRefresh: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSync: () -> Unit,
    onStatusFilterChange: (WorkOrderStatus?) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton && state.canRead)
    val visible = state.visibleOrders

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AtelierMainTopBar(
                title = stringResource(R.string.orders_list_title),
                onOpenNotifications = onOpenNotifications,
                unreadNotificationsCount = state.unreadNotificationsCount,
                onOpenSync = onOpenSync,
                pendingSyncCount = state.pendingSyncCount
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.isLoaded && state.orders.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .floatingBarPadding(),
                contentPadding = PaddingValues(bottom = Spacing.md)
            ) {
                item(key = "refresh") {
                    AtelierRefreshBar(visible = state.isRefreshing && state.orders.isNotEmpty())
                }
                if (!state.canRead) {
                    item(key = "no-permission") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.orders_no_permission),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    return@LazyColumn
                }
                item(key = "filters") {
                    Filters(state, onStatusFilterChange, onQueryChange)
                }
                if (state.showingCache && state.orders.isNotEmpty()) {
                    item(key = "cache") {
                        AtelierBanner(
                            message = if (state.isOnline) state.error?.asString() ?: stringResource(R.string.orders_cache_notice)
                            else stringResource(R.string.orders_cache_notice),
                            icon = Icons.Outlined.Info,
                            kind = BannerKind.Info,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                state.lastRefreshedAt?.let { refreshedAt ->
                    item(key = "updated") { UpdatedLine(refreshedAt) }
                }
                when {
                    showSkeleton -> items(6, key = { "bone-$it" }) {
                        AtelierListRow(title = "Orden", subtitle = "Cliente", plate = "ABC123", loading = true)
                    }
                    state.showSkeleton -> Unit
                    state.orders.isEmpty() && state.error != null && !state.isOnline -> item(key = "offline") {
                        AtelierOfflineState(onRetry = onRefresh, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    state.orders.isEmpty() && state.error != null -> item(key = "error") {
                        AtelierErrorState(
                            message = state.error.asString(),
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.orders.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.orders_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    visible.isEmpty() -> item(key = "empty-filtered") {
                        AtelierEmptyState(
                            text = stringResource(R.string.orders_empty_filtered),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(visible, key = { it.id }) { order ->
                        OrderRow(
                            order = order,
                            hasPendingSuspension = order.id in state.orderIdsWithPendingSuspension,
                            onClick = { onOpenOrder(order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Filters(
    state: WorkOrdersUiState,
    onStatusFilterChange: (WorkOrderStatus?) -> Unit,
    onQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AtelierTextField(
            label = stringResource(R.string.orders_search_label),
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.Search,
            trailing = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.orders_search_clear))
                    }
                }
            } else null
        )
        val filterLabels = FilterOptions.associateWith { stringResource(it.filterSegmentLabelRes()) }
        AtelierSegmentedControl(
            options = FilterOptions,
            selected = state.statusFilter,
            onSelect = onStatusFilterChange,
            label = { filterLabels.getValue(it) },
            badge = { status ->
                if (status == null) 0 else state.orders.count { it.status == status }
            }
        )
    }
}

@Composable
private fun UpdatedLine(refreshedAt: Instant) {
    val minutes = remember(refreshedAt) { Duration.between(refreshedAt, Instant.now()).toMinutes().toInt().coerceAtLeast(0) }
    Text(
        text = if (minutes == 0) stringResource(R.string.orders_updated_now)
        else pluralStringResource(R.plurals.orders_updated_ago, minutes, minutes),
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun OrderRow(order: WorkOrder, hasPendingSuspension: Boolean, onClick: () -> Unit) {
    val number = order.internalNumber?.let { stringResource(R.string.orders_order_number, it) }
        ?: stringResource(R.string.orders_no_number)
    AtelierListRow(
        title = order.customerName ?: number,
        subtitle = listOfNotNull(number.takeIf { order.customerName != null }, order.bayName)
            .joinToString(" · ")
            .ifEmpty { null },
        plate = order.plate,
        onClick = onClick,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPendingSuspension) {
                    AtelierChip(
                        text = stringResource(R.string.orders_pending_suspension_chip),
                        kind = ChipKind.Warn
                    )
                }
                AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind())
            }
        }
    )
}

// region Previews

private fun previewOrder(
    id: String, number: Int, plate: String, customer: String, status: WorkOrderStatus, bay: String?
) = WorkOrder(
    id = id, internalNumber = number, branchId = "b1", vehicleId = "v$id", customerId = "c$id",
    currentBayId = bay?.let { "bay" }, bayName = bay, status = status, diagnosticSummary = null, mileageIn = null,
    subtotal = null, taxAmount = null, totalAmount = Money.of("480.00", Money.PEN), plate = plate, customerName = customer
)

private val previewState = WorkOrdersUiState(
    isLoaded = true,
    canCreate = true,
    lastRefreshedAt = Instant.now().minusSeconds(240),
    pendingSyncCount = 2,
    unreadNotificationsCount = 3,
    orders = listOf(
        previewOrder("1", 1042, "BKR418", "Rosa Quispe Mamani", WorkOrderStatus.DRAFT, null),
        previewOrder("2", 1041, "F5U731", "Transportes Huamán S.A.C.", WorkOrderStatus.IN_PROGRESS, "Bahía 2"),
        previewOrder("3", 1040, "AXT209", "Luis Alberto Paredes", WorkOrderStatus.IN_PROGRESS, "Bahía 1"),
        previewOrder("4", 1038, "D2C604", "Carmen Flores", WorkOrderStatus.COMPLETED, null)
    )
)

@Composable
private fun PreviewContent(state: WorkOrdersUiState) {
    WorkOrdersContent(state, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Órdenes · claro", widthDp = 390, heightDp = 844)
@Composable
private fun WorkOrdersPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Órdenes · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkOrdersPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Órdenes · cargando sin caché", widthDp = 390, heightDp = 844)
@Composable
private fun WorkOrdersPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(WorkOrdersUiState(isRefreshing = true, canCreate = true))
}

// endregion
