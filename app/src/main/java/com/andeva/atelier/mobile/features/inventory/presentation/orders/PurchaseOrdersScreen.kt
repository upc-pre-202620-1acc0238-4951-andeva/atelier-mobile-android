package com.andeva.atelier.mobile.features.inventory.presentation.orders

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLine
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.presentation.chipKind
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import java.math.BigDecimal
import java.time.Instant

/** US34 E2: órdenes de compra por estado y alta de borrador (`PURCHASE_ORDERS_CREATE`). Requiere `PURCHASE_ORDERS_READ`. */
@Composable
fun PurchaseOrdersScreen(
    onBack: () -> Unit,
    onOpenOrder: (purchaseOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.createdOrderId) {
        val id = state.createdOrderId ?: return@LaunchedEffect
        viewModel.onCreatedOrderNavigated()
        onOpenOrder(id)
    }

    LaunchedEffect(state.hapticEvent) {
        if (state.hapticEvent > 0) {
            if (state.hapticIsError) haptics.reject() else haptics.confirm()
        }
    }

    PurchaseOrdersContent(
        state = state,
        onBack = onBack,
        onStatusClick = viewModel::onStatusClick,
        onRefresh = viewModel::refresh,
        onOpenOrder = onOpenOrder,
        onOpenCreateSheet = viewModel::onOpenCreateSheet,
        onDismissCreateSheet = viewModel::onDismissCreateSheet,
        onSelectSupplier = viewModel::onSelectSupplier,
        onPartQueryChange = viewModel::onPartQueryChange,
        onToggleLowStockOnly = viewModel::onToggleLowStockOnly,
        onSelectPart = viewModel::onSelectPart,
        onLineQuantityChange = viewModel::onLineQuantityChange,
        onLineUnitCostChange = viewModel::onLineUnitCostChange,
        onOrderNumberChange = viewModel::onOrderNumberChange,
        onNotesChange = viewModel::onNotesChange,
        onCreateOrder = viewModel::onCreateOrder,
        onCreateLowStockOrder = viewModel::onCreateLowStockOrder,
        modifier = modifier
    )
}

private val statusFilters = listOf(
    PurchaseOrderStatus.DRAFT to R.string.inv_seg2_po_filter_draft,
    PurchaseOrderStatus.ISSUED to R.string.inventory_po_filter_issued,
    PurchaseOrderStatus.RECEIVED to R.string.inventory_po_filter_received,
    PurchaseOrderStatus.CANCELED to R.string.inv_seg2_po_filter_canceled,
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
    onOpenCreateSheet: () -> Unit,
    onDismissCreateSheet: () -> Unit,
    onSelectSupplier: (String) -> Unit,
    onPartQueryChange: (String) -> Unit = {},
    onToggleLowStockOnly: () -> Unit = {},
    onSelectPart: (Part) -> Unit = {},
    onLineQuantityChange: (String) -> Unit = {},
    onLineUnitCostChange: (String) -> Unit = {},
    onOrderNumberChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onCreateOrder: () -> Unit,
    onCreateLowStockOrder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val delayedLoading = rememberDelayedLoading(state.orders.isEmpty() && state.isLoading)
    val showSkeleton = state.orders.isEmpty() && state.isLoading && (LocalInspectionMode.current || delayedLoading)

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canRead && state.canCreate) {
                AtelierFab(
                    text = stringResource(R.string.inv_seg2_po_new_fab),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenCreateSheet,
                    modifier = Modifier.floatingBarPadding()
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = (if (state.canCreate) 96.dp else 24.dp) + LocalFloatingBarInset.current
                    )
                ) {
                    item(key = "filters") {
                        val labels = statusFilters.associate { (status, labelRes) -> status to stringResource(labelRes) }
                        AtelierSegmentedControl(
                            options = statusFilters.map { it.first },
                            selected = state.status,
                            onSelect = onStatusClick,
                            label = { labels.getValue(it) },
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        )
                    }
                    val error = state.error
                    when {
                        showSkeleton -> items(4, key = { "bone-$it" }) {
                            AtelierListRow(
                                title = "Orden OC-0001",
                                subtitle = "3 líneas · S/ 1,250.00",
                                loading = true
                            )
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
                        state.orders.isEmpty() && !state.isLoading -> item(key = "empty") {
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

    if (state.createSheetOpen) {
        CreatePurchaseOrderSheet(
            state = state,
            onDismiss = onDismissCreateSheet,
            onSelectSupplier = onSelectSupplier,
            onPartQueryChange = onPartQueryChange,
            onToggleLowStockOnly = onToggleLowStockOnly,
            onSelectPart = onSelectPart,
            onLineQuantityChange = onLineQuantityChange,
            onLineUnitCostChange = onLineUnitCostChange,
            onOrderNumberChange = onOrderNumberChange,
            onNotesChange = onNotesChange,
            onConfirm = onCreateOrder,
            onConfirmLowStock = onCreateLowStockOrder
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatePurchaseOrderSheet(
    state: PurchaseOrdersUiState,
    onDismiss: () -> Unit,
    onSelectSupplier: (String) -> Unit,
    onPartQueryChange: (String) -> Unit,
    onToggleLowStockOnly: () -> Unit,
    onSelectPart: (Part) -> Unit,
    onLineQuantityChange: (String) -> Unit,
    onLineUnitCostChange: (String) -> Unit,
    onOrderNumberChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onConfirmLowStock: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_po_create_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_po_create_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && state.selectedSupplierId != null && state.selectedPartId != null && !state.isCreating,
                loading = state.isCreating
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isCreating
            )
        }
    ) {
        Text(
            text = stringResource(R.string.inv_seg2_po_select_supplier),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        when {
            state.isLoadingSuppliers -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.suppliersError != null -> AtelierBanner(
                message = state.suppliersError.asString(),
                icon = Icons.Outlined.ErrorOutline
            )
            state.suppliers.isEmpty() -> AtelierBanner(
                message = stringResource(R.string.inv_seg2_po_no_suppliers),
                icon = Icons.Outlined.ErrorOutline,
                kind = BannerKind.Wait
            )
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.suppliers.forEach { supplier ->
                    FilterChip(
                        selected = state.selectedSupplierId == supplier.id,
                        onClick = { onSelectSupplier(supplier.id) },
                        label = { Text(supplier.businessName) },
                        shape = ControlShape,
                        enabled = !state.isCreating
                    )
                }
            }
        }
        if (state.lowStockParts.isNotEmpty() && state.suppliers.isNotEmpty()) {
            AtelierButton(
                text = stringResource(R.string.inv_seg2_po_generate_low_stock_btn, state.lowStockParts.size),
                onClick = onConfirmLowStock,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                icon = Icons.Outlined.WarningAmber,
                enabled = state.isOnline && state.selectedSupplierId != null && !state.isCreating,
                loading = state.isCreating
            )
            Text(
                text = stringResource(R.string.inv_seg2_po_generate_low_stock_hint),
                style = MaterialTheme.typography.labelSmall,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        Text(
            text = stringResource(R.string.inv_seg2_po_initial_line_title),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        if (state.catalogParts.isEmpty()) {
            AtelierBanner(
                message = stringResource(R.string.inv_seg2_po_no_parts),
                icon = Icons.Outlined.ErrorOutline,
                kind = BannerKind.Wait
            )
        } else {
            AtelierTextField(
                label = stringResource(R.string.inv_seg2_po_select_part),
                value = state.partQuery,
                onValueChange = onPartQueryChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Outlined.Search,
                placeholder = stringResource(R.string.inv_seg2_po_part_search_hint),
                enabled = !state.isCreating
            )
            if (state.lowStockParts.isNotEmpty()) {
                val atelier = AtelierTheme.colors
                FilterChip(
                    selected = state.lowStockOnly,
                    onClick = onToggleLowStockOnly,
                    label = { Text(stringResource(R.string.inv_seg2_po_low_stock_only, state.lowStockParts.size)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = atelier.waitContainer,
                        selectedLabelColor = atelier.wait,
                        selectedLeadingIconColor = atelier.wait,
                        iconColor = atelier.wait
                    ),
                    shape = ControlShape,
                    enabled = !state.isCreating
                )
            }
            if (state.filteredCatalogParts.isEmpty()) {
                Text(
                    text = stringResource(R.string.inv_seg2_po_low_stock_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.filteredCatalogParts.forEach { part ->
                        PurchaseOrderPartChip(
                            part = part,
                            selected = state.selectedPartId == part.id,
                            enabled = !state.isCreating,
                            onClick = { onSelectPart(part) }
                        )
                    }
                }
            }
            AtelierTextField(
                label = stringResource(R.string.inv_seg2_batch_quantity_label),
                value = state.lineQuantity,
                onValueChange = onLineQuantityChange,
                modifier = Modifier.fillMaxWidth(),
                isError = state.lineQtyError != null,
                errorText = state.lineQtyError?.let { stringResource(it) },
                enabled = !state.isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            AtelierTextField(
                label = stringResource(R.string.inv_seg2_batch_unit_cost_label),
                value = state.lineUnitCost,
                onValueChange = onLineUnitCostChange,
                modifier = Modifier.fillMaxWidth(),
                isError = state.lineCostError != null,
                errorText = state.lineCostError?.let { stringResource(it) },
                enabled = !state.isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_po_number_optional),
            value = state.orderNumber,
            onValueChange = onOrderNumberChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isCreating
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_po_notes_optional),
            value = state.notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isCreating
        )
        state.createError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
internal fun PurchaseOrderPartChip(
    part: Part,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val atelier = AtelierTheme.colors
    val stockText = part.totalStock.stripTrailingZeros().toPlainString()
    val minText = part.minimumStock.stripTrailingZeros().toPlainString()
    val label = when {
        part.isOutOfStock -> stringResource(R.string.inv_seg2_po_part_chip_out_of_stock, part.name, part.sku, minText)
        part.isLowStock -> stringResource(R.string.inv_seg2_po_part_chip_low_stock, part.name, part.sku, stockText, minText)
        else -> "${part.name} (${part.sku})"
    }
    val tintInk = if (part.isOutOfStock) atelier.bad else atelier.wait
    val tintBg = if (part.isOutOfStock) atelier.badContainer else atelier.waitContainer

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (part.isLowStock) {
            {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = if (part.isLowStock) {
            FilterChipDefaults.filterChipColors(
                containerColor = tintBg,
                labelColor = tintInk,
                iconColor = tintInk,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = tintInk
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
        border = if (part.isLowStock && !selected) {
            FilterChipDefaults.filterChipBorder(
                enabled = enabled,
                selected = false,
                borderColor = tintInk.copy(alpha = 0.55f)
            )
        } else {
            FilterChipDefaults.filterChipBorder(enabled = enabled, selected = selected)
        },
        shape = ControlShape,
        enabled = enabled
    )
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
    canCreate = true,
    orders = listOf(
        order("1", "OC-0042", PurchaseOrderStatus.ISSUED, "1250.00"),
        order("2", "OC-0041", PurchaseOrderStatus.ISSUED, "480.50", lines = 1),
        order("3", "OC-0039", PurchaseOrderStatus.RECEIVED, "2310.00", lines = 5)
    )
)

@Composable
private fun PurchaseOrdersPreviewHost(state: PurchaseOrdersUiState) = PurchaseOrdersContent(
    state = state, onBack = {}, onStatusClick = {}, onRefresh = {}, onOpenOrder = {},
    onOpenCreateSheet = {}, onDismissCreateSheet = {}, onSelectSupplier = {},
    onOrderNumberChange = {}, onNotesChange = {}, onCreateOrder = {}
)

@Preview(name = "Compras · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PurchaseOrdersPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    PurchaseOrdersPreviewHost(previewState)
}

@Preview(name = "Compras · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseOrdersPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PurchaseOrdersPreviewHost(previewState)
}

@Preview(name = "Compras · vacío", widthDp = 390, heightDp = 600)
@Composable
private fun PurchaseOrdersPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PurchaseOrdersPreviewHost(PurchaseOrdersUiState(isLoading = false))
}
