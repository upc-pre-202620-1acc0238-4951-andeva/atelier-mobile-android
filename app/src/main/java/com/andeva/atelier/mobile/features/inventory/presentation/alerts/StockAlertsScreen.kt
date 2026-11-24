package com.andeva.atelier.mobile.features.inventory.presentation.alerts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import java.math.BigDecimal

/**
 * US34 E1: repuestos en el mínimo o debajo. Tocar uno abre el repuesto. Sin botón falso de resolver (H13);
 * ofrece crear orden de compra (`PURCHASE_ORDERS_CREATE`). Requiere `ALERTS_READ`.
 */
@Composable
fun StockAlertsScreen(
    onBack: () -> Unit,
    onOpenPart: (partId: String) -> Unit,
    modifier: Modifier = Modifier,
    onCreatePurchaseOrder: () -> Unit = {},
    viewModel: StockAlertsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StockAlertsContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenPart = onOpenPart,
        onCreatePurchaseOrder = onCreatePurchaseOrder,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAlertsContent(
    state: StockAlertsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPart: (String) -> Unit,
    onCreatePurchaseOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val delayedLoading = rememberDelayedLoading(state.alerts.isEmpty() && state.isRefreshing)
    val showSkeleton = state.alerts.isEmpty() && state.isRefreshing && (LocalInspectionMode.current || delayedLoading)

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canRead && state.canCreatePurchaseOrder) {
                AtelierFab(
                    text = stringResource(R.string.inv_seg2_alerts_create_po_fab),
                    icon = Icons.Outlined.ShoppingCart,
                    onClick = onCreatePurchaseOrder
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AtelierTopBar(title = stringResource(R.string.inventory_alerts_title), onBack = onBack)
            if (!state.canRead) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.inventory_alerts_no_permission),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            AtelierRefreshBar(visible = state.isRefreshing && state.alerts.isNotEmpty())
            PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (state.canCreatePurchaseOrder) 96.dp else 24.dp)
                ) {
                    if (!state.isOnline) {
                        item(key = "offline") {
                            AtelierBanner(
                                message = stringResource(R.string.inventory_alerts_offline),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    item(key = "header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.inventory_alerts_header),
                            count = state.alerts.size.takeIf { it > 0 },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    val error = state.error
                    when {
                        showSkeleton -> items(4, key = { "bone-$it" }) {
                            AtelierListRow(
                                title = "Filtro de aceite",
                                subtitle = "SKU FLT-001 · 2 de 6",
                                loading = true
                            )
                        }
                        state.alerts.isEmpty() && error != null -> item(key = "error") {
                            AtelierErrorState(
                                message = error.asString(),
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        state.alerts.isEmpty() && !state.isRefreshing -> item(key = "empty") {
                            AtelierEmptyState(
                                text = stringResource(R.string.inventory_alerts_empty),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        else -> items(state.alerts, key = { it.itemId }) { alert ->
                            AlertRow(alert, onClick = { onOpenPart(alert.itemId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: StockAlert, onClick: () -> Unit) {
    val colors = AtelierTheme.colors
    val out = alert.currentStock <= BigDecimal.ZERO
    val minStock = alert.minimumStock.toFloat().coerceAtLeast(1f)
    val currentStock = alert.currentStock.toFloat().coerceAtLeast(0f)
    val progress = (currentStock / minStock).coerceIn(0f, 1f)

    AtelierSurface(
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AtelierListRow(
                title = alert.name,
                subtitle = stringResource(
                    R.string.inventory_alert_subtitle,
                    alert.sku,
                    alert.currentStock.formatQuantity(),
                    alert.minimumStock.formatQuantity()
                ),
                showDivider = false,
                trailing = {
                    if (out) {
                        AtelierChip(text = stringResource(R.string.stock_out), kind = ChipKind.Bad)
                    } else {
                        AtelierChip(
                            text = stringResource(R.string.inventory_alert_missing, alert.deficit.formatQuantity()),
                            kind = ChipKind.Warn
                        )
                    }
                }
            )
            AtelierMetricBar(
                progress = progress,
                color = if (out) colors.badMark else colors.warnMark,
                height = 6.dp,
                markerAt = 1f,
                modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)
            )
        }
    }
}

private fun alert(id: String, name: String, current: String, min: String) = StockAlert(
    itemId = id, name = name, sku = "SKU-$id", category = ItemCategory.FILTERS, currentStock = BigDecimal(current),
    minimumStock = BigDecimal(min), deficit = (BigDecimal(min) - BigDecimal(current)).max(BigDecimal.ZERO)
)

private val previewState = StockAlertsUiState(
    isRefreshing = false,
    canCreatePurchaseOrder = true,
    alerts = listOf(
        alert("1", "Líquido de frenos DOT4", "0", "6"),
        alert("2", "Filtro de aceite Bosch", "2", "8"),
        alert("3", "Pastillas de freno delanteras", "5", "5")
    )
)

@Preview(name = "Alertas · claro", widthDp = 390, heightDp = 844)
@Composable
private fun StockAlertsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    StockAlertsContent(previewState, {}, {}, {}, {})
}

@Preview(name = "Alertas · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StockAlertsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    StockAlertsContent(previewState.copy(isOnline = false), {}, {}, {}, {})
}

@Preview(name = "Alertas · vacío", widthDp = 390, heightDp = 600)
@Composable
private fun StockAlertsPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    StockAlertsContent(StockAlertsUiState(isRefreshing = false), {}, {}, {}, {})
}
