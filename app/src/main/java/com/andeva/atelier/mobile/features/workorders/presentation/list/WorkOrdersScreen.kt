package com.andeva.atelier.mobile.features.workorders.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.math.BigDecimal

/** Órdenes de la sede. [onOpenOrder] recibe el id de la orden para ver sus tareas. */
@Composable
fun WorkOrdersScreen(
    onBack: () -> Unit,
    onOpenOrder: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkOrdersContent(state, onBack, viewModel::refresh, onOpenOrder, modifier)
}

@Composable
internal fun WorkOrdersContent(
    state: WorkOrdersUiState,
    onBack: () -> Unit,
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
        AtelierTopBar(title = stringResource(R.string.orders_title), onBack = onBack)
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
                else -> state.orders.forEach { order -> OrderCard(order, onClick = { onOpenOrder(order.id) }) }
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
}

@Composable
private fun OrderCard(order: WorkOrder, onClick: () -> Unit) {
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
        AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind())
        Text(
            text = order.internalNumber?.let { stringResource(R.string.task_card_work_order, it) }
                ?: stringResource(R.string.orders_no_number),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        order.plate?.let {
            Text(
                text = stringResource(R.string.task_card_plate, it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
            orders = listOf(previewOrder("1", 1, WorkOrderStatus.IN_PROGRESS), previewOrder("2", 2, WorkOrderStatus.COMPLETED))
        ),
        onBack = {}, onRefresh = {}, onOpenOrder = {}
    )
}
