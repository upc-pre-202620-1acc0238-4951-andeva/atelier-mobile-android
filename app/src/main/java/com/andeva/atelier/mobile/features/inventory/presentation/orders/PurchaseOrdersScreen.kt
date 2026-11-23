package com.andeva.atelier.mobile.features.inventory.presentation.orders

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLine
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.presentation.chipKind
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import java.math.BigDecimal
import java.time.Instant

/** US34 E2: órdenes de compra por recibir (o ya recibidas). Requiere `PURCHASE_ORDERS_READ`. */
@Composable
fun PurchaseOrdersScreen(
    onBack: () -> Unit,
    onOpenOrder: (purchaseOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PurchaseOrdersContent(state, onBack, viewModel::onStatusClick, viewModel::refresh, onOpenOrder, modifier)
}

private val statusFilters = listOf(
    PurchaseOrderStatus.ISSUED to R.string.inventory_po_filter_issued,
    PurchaseOrderStatus.RECEIVED to R.string.inventory_po_filter_received,
    null to R.string.inventory_po_filter_all
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrdersContent(
    state: PurchaseOrdersUiState,
    onBack: () -> Unit,
    onStatusClick: (PurchaseOrderStatus?) -> Unit,
    onRefresh: () -> Unit,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.inventory_po_title), onBack = onBack)
        if (!state.canRead) {
            AtelierNoPermissionState(
                text = stringResource(R.string.inventory_po_no_permission),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        AtelierRefreshBar(visible = state.isLoading && state.orders.isNotEmpty())
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "filters") {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusFilters.forEach { (status, label) ->
                            FilterChip(
                                selected = state.status == status,
                                onClick = { onStatusClick(status) },
                                label = { Text(stringResource(label)) },
                                shape = ControlShape
                            )
                        }
                    }
                }
                val error = state.error
                when {
                    state.orders.isEmpty() && state.isLoading -> items(4, key = { "bone-$it" }) {
                        AtelierListRow(title = "Orden OC-0001", subtitle = "3 líneas · S/ 1,250.00", loading = true)
                    }
                    state.orders.isEmpty() && error != null && !state.isOnline -> item(key = "offline") {
                        AtelierOfflineState(onRetry = onRefresh, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    state.orders.isEmpty() && error != null -> item(key = "error") {
                        AtelierErrorState(
                            message = error.asString(),
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.orders.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.inventory_po_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(state.orders, key = { it.id }) { order ->
                        OrderRow(order, onClick = { onOpenOrder(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: PurchaseOrder, onClick: () -> Unit) {
    val lines = pluralStringResource(R.plurals.inventory_po_lines, order.lines.size, order.lines.size)
    AtelierListRow(
        title = order.orderNumber?.let { stringResource(R.string.inventory_po_number, it) }
            ?: stringResource(R.string.inventory_po_no_number),
        subtitle = listOfNotNull(
            lines,
            order.totalCost?.format(),
            order.receivedAt?.let { stringResource(R.string.inventory_po_received_on, it.formatLimaDate()) }
        ).joinToString(" · "),
        onClick = onClick,
        trailing = { AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind()) }
    )
}

private fun order(id: String, number: String, status: PurchaseOrderStatus, total: String, lines: Int = 3) = PurchaseOrder(
    id = id, orderNumber = number, supplierId = "s1", branchId = "b1", status = status,
    totalCost = Money(BigDecimal(total), Money.PEN), receiptNumber = null, receiptImageUrl = null,
    receivedAt = if (status == PurchaseOrderStatus.RECEIVED) Instant.parse("2026-11-10T15:00:00Z") else null,
    lines = List(lines) {
        PurchaseOrderLine("l$it", "p$it", BigDecimal("4"), Money(BigDecimal("52.00"), Money.PEN), Money(BigDecimal("208.00"), Money.PEN))
    }
)

private val previewState = PurchaseOrdersUiState(
    isLoading = false,
    orders = listOf(
        order("1", "OC-0042", PurchaseOrderStatus.ISSUED, "1250.00"),
        order("2", "OC-0041", PurchaseOrderStatus.ISSUED, "480.50", lines = 1),
        order("3", "OC-0039", PurchaseOrderStatus.RECEIVED, "2310.00", lines = 5)
    )
)

@Preview(name = "Compras · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PurchaseOrdersPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    PurchaseOrdersContent(previewState, {}, {}, {}, {})
}

@Preview(name = "Compras · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseOrdersPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PurchaseOrdersContent(previewState, {}, {}, {}, {})
}

@Preview(name = "Compras · vacío", widthDp = 390, heightDp = 600)
@Composable
private fun PurchaseOrdersPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PurchaseOrdersContent(PurchaseOrdersUiState(isLoading = false), {}, {}, {}, {})
}
