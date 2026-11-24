package com.andeva.atelier.mobile.features.inventory.presentation.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import com.andeva.atelier.mobile.features.inventory.presentation.stockChipKind
import com.andeva.atelier.mobile.features.inventory.presentation.stockLabelRes
import java.math.BigDecimal
import java.time.Instant

/**
 * US32: ficha del repuesto, lotes en orden FIFO, edición (`PARTS_MANAGE`) y operaciones de lote FIFO.
 */
@Composable
fun PartDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPurchaseOrders: (() -> Unit)? = null,
    viewModel: PartDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.hapticEvent) {
        if (state.hapticEvent > 0) {
            if (state.hapticIsError) haptics.reject() else haptics.confirm()
        }
    }

    PartDetailContent(
        state = state,
        onBack = onBack,
        onRefresh = { viewModel.refresh(force = true) },
        onOpenEditPart = viewModel::onOpenEditPart,
        onOpenReceiveBatch = viewModel::onOpenReceiveBatch,
        onOpenDispatchFifo = viewModel::onOpenDispatchFifo,
        onOpenRestoreBatch = viewModel::onOpenRestoreBatch,
        onDismissSheet = viewModel::onDismissSheet,
        onEditNameChange = viewModel::onEditNameChange,
        onEditCategoryChange = viewModel::onEditCategoryChange,
        onEditBasePriceChange = viewModel::onEditBasePriceChange,
        onEditMinStockChange = viewModel::onEditMinStockChange,
        onEditStatusChange = viewModel::onEditStatusChange,
        onConfirmEditPart = viewModel::onConfirmEditPart,
        onBatchNumberChange = viewModel::onBatchNumberChange,
        onBatchQuantityChange = viewModel::onBatchQuantityChange,
        onBatchUnitCostChange = viewModel::onBatchUnitCostChange,
        onSelectedBatchIdChange = viewModel::onSelectedBatchIdChange,
        onRestoreReasonChange = viewModel::onRestoreReasonChange,
        onConfirmReceiveBatch = viewModel::onConfirmReceiveBatch,
        onConfirmDispatchFifo = viewModel::onConfirmDispatchFifo,
        onConfirmRestoreBatch = viewModel::onConfirmRestoreBatch,
        onOpenPurchaseOrders = onOpenPurchaseOrders,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PartDetailContent(
    state: PartDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenEditPart: () -> Unit,
    onOpenReceiveBatch: () -> Unit,
    onOpenDispatchFifo: () -> Unit,
    onOpenRestoreBatch: () -> Unit,
    onDismissSheet: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditCategoryChange: (ItemCategory) -> Unit,
    onEditBasePriceChange: (String) -> Unit,
    onEditMinStockChange: (String) -> Unit,
    onEditStatusChange: (PartStatus) -> Unit,
    onConfirmEditPart: () -> Unit,
    onBatchNumberChange: (String) -> Unit,
    onBatchQuantityChange: (String) -> Unit,
    onBatchUnitCostChange: (String) -> Unit,
    onSelectedBatchIdChange: (String?) -> Unit,
    onRestoreReasonChange: (String) -> Unit,
    onConfirmReceiveBatch: () -> Unit,
    onConfirmDispatchFifo: () -> Unit,
    onConfirmRestoreBatch: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPurchaseOrders: (() -> Unit)? = null
) {
    val part = state.part
    val delayedBatchBones = rememberDelayedLoading(state.batches.isEmpty() && state.isLoading)
    val showBatchBones = state.batches.isEmpty() && state.isLoading && (LocalInspectionMode.current || delayedBatchBones)
    val delayedPartBones = rememberDelayedLoading(part == null && state.isLoading)
    val showPartBones = part == null && state.isLoading && (LocalInspectionMode.current || delayedPartBones)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AtelierFichaPanel(
            onBack = onBack,
            title = part?.name ?: stringResource(R.string.part_detail_title),
            subtitle = part?.let { stringResource(R.string.part_sku, it.sku) },
            status = part?.let { { AtelierChip(text = stringResource(it.stockLabelRes()), kind = it.stockChipKind()) } },
            actions = {
                if (state.canManageParts && part != null) {
                    IconButton(onClick = onOpenEditPart) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.inv_seg2_part_edit_btn)
                        )
                    }
                }
            }
        )
        AtelierRefreshBar(visible = state.isLoading && part != null)
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                state.actionBanner?.let { msg ->
                    item(key = "action-banner") {
                        AtelierBanner(
                            message = msg.asString(),
                            icon = Icons.Outlined.CheckCircle,
                            kind = BannerKind.Ok,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        )
                    }
                }
                if (state.showingCache) {
                    item(key = "cache") {
                        AtelierBanner(
                            message = state.error?.asString() ?: stringResource(R.string.batches_cache_notice),
                            icon = Icons.Outlined.Info,
                            kind = BannerKind.Info,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                if (showPartBones) {
                    item(key = "facts-bone-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.inventory_detail_stock_section),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    items(3, key = { "fact-bone-$it" }) {
                        AtelierListRow(title = "Existencias", subtitle = "14 unidades", loading = true)
                    }
                } else if (part != null) {
                    if (part.isLowStock || part.isOutOfStock) {
                        item(key = "low-stock-alert-banner") {
                            Column(
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AtelierBanner(
                                    message = stringResource(
                                        R.string.inv_seg2_part_low_stock_banner,
                                        part.totalStock.formatQuantity(),
                                        part.minimumStock.formatQuantity()
                                    ),
                                    icon = Icons.Outlined.WarningAmber,
                                    kind = if (part.isOutOfStock) BannerKind.Bad else BannerKind.Wait
                                )
                                if (state.canOpenPurchaseOrders && onOpenPurchaseOrders != null) {
                                    AtelierButton(
                                        text = stringResource(R.string.inv_seg2_part_open_po_btn),
                                        onClick = onOpenPurchaseOrders,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = AtelierButtonStyle.Outlined,
                                        icon = Icons.Outlined.ShoppingCart
                                    )
                                }
                            }
                        }
                    }
                    item(key = "facts-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.inventory_detail_stock_section),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item(key = "stock-calibration") {
                        StockCalibrationCard(
                            part = part,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        )
                    }
                    item(key = "facts-card") {
                        AtelierSurface(
                            tier = SurfaceTier.Card,
                            shape = CardLargeShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        ) {
                            Column {
                                Fact(
                                    labelRes = R.string.inventory_detail_stock,
                                    value = part.totalStock.formatQuantity(),
                                    showDivider = true
                                )
                                Fact(
                                    labelRes = R.string.inventory_detail_minimum,
                                    value = part.minimumStock.formatQuantity(),
                                    showDivider = part.basePrice != null || part.category != null
                                )
                                part.basePrice?.let { price ->
                                    Fact(
                                        labelRes = R.string.inventory_detail_price,
                                        value = price.format(),
                                        showDivider = part.category != null
                                    )
                                }
                                part.category?.let { category ->
                                    Fact(
                                        labelRes = R.string.inventory_detail_category,
                                        value = stringResource(category.labelRes()),
                                        showDivider = false
                                    )
                                }
                            }
                        }
                    }
                    if (state.hasAnyBatchAction) {
                        item(key = "batch-actions") {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AtelierSectionHeader(title = stringResource(R.string.inv_seg2_batch_actions_header))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (state.canReceiveBatch) {
                                        AtelierButton(
                                            text = stringResource(R.string.inv_seg2_batch_receive_btn),
                                            onClick = onOpenReceiveBatch,
                                            style = AtelierButtonStyle.Tonal,
                                            icon = Icons.Outlined.AddBox
                                        )
                                    }
                                    if (state.canDispatchFifo) {
                                        AtelierButton(
                                            text = stringResource(R.string.inv_seg2_batch_dispatch_btn),
                                            onClick = onOpenDispatchFifo,
                                            style = AtelierButtonStyle.Outlined,
                                            enabled = !part.isOutOfStock,
                                            icon = Icons.Outlined.Output
                                        )
                                    }
                                    if (state.canRestoreBatch) {
                                        AtelierButton(
                                            text = stringResource(R.string.inv_seg2_batch_restore_btn),
                                            onClick = onOpenRestoreBatch,
                                            style = AtelierButtonStyle.Outlined,
                                            icon = Icons.Outlined.Restore
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "batches-header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.batches_title),
                        count = if (state.canReadBatches && state.batches.isNotEmpty()) state.batches.size else null,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                when {
                    !state.canReadBatches -> item(key = "batches-no-permission") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.batches_no_permission),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    showBatchBones -> items(2, key = { "bone-$it" }) {
                        AtelierListRow(title = "Lote L-0001", subtitle = "Saldo: 6 · Ingresó el 10/03/2026", loading = true)
                    }
                    state.batches.isEmpty() -> item(key = "batches-empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.batches_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(state.batches, key = { it.id }) { batch ->
                        BatchRow(batch, isNext = batch.id == state.nextBatchId)
                    }
                }
            }
        }
    }

    when (state.activeSheet) {
        PartDetailSheet.EDIT_PART -> EditPartSheet(
            state = state,
            onDismiss = onDismissSheet,
            onNameChange = onEditNameChange,
            onCategoryChange = onEditCategoryChange,
            onBasePriceChange = onEditBasePriceChange,
            onMinStockChange = onEditMinStockChange,
            onStatusChange = onEditStatusChange,
            onConfirm = onConfirmEditPart
        )
        PartDetailSheet.RECEIVE_BATCH -> ReceiveDirectBatchSheet(
            state = state,
            onDismiss = onDismissSheet,
            onBatchNumberChange = onBatchNumberChange,
            onQuantityChange = onBatchQuantityChange,
            onUnitCostChange = onBatchUnitCostChange,
            onConfirm = onConfirmReceiveBatch
        )
        PartDetailSheet.DISPATCH_FIFO -> DispatchFifoSheet(
            state = state,
            onDismiss = onDismissSheet,
            onQuantityChange = onBatchQuantityChange,
            onConfirm = onConfirmDispatchFifo
        )
        PartDetailSheet.RESTORE_BATCH -> RestoreBatchSheet(
            state = state,
            onDismiss = onDismissSheet,
            onQuantityChange = onBatchQuantityChange,
            onSelectBatch = onSelectedBatchIdChange,
            onReasonChange = onRestoreReasonChange,
            onConfirm = onConfirmRestoreBatch
        )
        PartDetailSheet.NONE -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPartSheet(
    state: PartDetailUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ItemCategory) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onMinStockChange: (String) -> Unit,
    onStatusChange: (PartStatus) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_part_edit_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_save),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !state.isSubmitting,
                loading = state.isSubmitting
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isSubmitting
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_name),
            value = state.editName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.editNameError != null,
            errorText = state.editNameError?.let { stringResource(it) },
            enabled = !state.isSubmitting
        )
        Text(
            text = stringResource(R.string.inv_seg2_part_category),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ItemCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.editCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(stringResource(cat.labelRes())) },
                    shape = ControlShape,
                    enabled = !state.isSubmitting
                )
            }
        }
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_base_price),
            value = state.editBasePrice,
            onValueChange = onBasePriceChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.editPriceError != null,
            errorText = state.editPriceError?.let { stringResource(it) },
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_min_stock),
            value = state.editMinStock,
            onValueChange = onMinStockChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.editMinStockError != null,
            errorText = state.editMinStockError?.let { stringResource(it) },
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Text(
            text = stringResource(R.string.inv_seg2_part_status),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PartStatus.entries.forEach { status ->
                val label = when (status) {
                    PartStatus.ACTIVE -> R.string.inv_seg2_part_status_active
                    PartStatus.INACTIVE -> R.string.inv_seg2_part_status_inactive
                    PartStatus.DISCONTINUED -> R.string.inv_seg2_part_status_discontinued
                }
                FilterChip(
                    selected = state.editStatus == status,
                    onClick = { onStatusChange(status) },
                    label = { Text(stringResource(label)) },
                    shape = ControlShape,
                    enabled = !state.isSubmitting
                )
            }
        }
        state.sheetError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun ReceiveDirectBatchSheet(
    state: PartDetailUiState,
    onDismiss: () -> Unit,
    onBatchNumberChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitCostChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_batch_receive_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !state.isSubmitting,
                loading = state.isSubmitting
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isSubmitting
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_number_label),
            value = state.batchNumber,
            onValueChange = onBatchNumberChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.batchNumberError != null,
            errorText = state.batchNumberError?.let { stringResource(it) },
            enabled = !state.isSubmitting
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_quantity_label),
            value = state.batchQuantity,
            onValueChange = onQuantityChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.batchQtyError != null,
            errorText = state.batchQtyError?.let { stringResource(it) },
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_unit_cost_label),
            value = state.batchUnitCost,
            onValueChange = onUnitCostChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.batchCostError != null,
            errorText = state.batchCostError?.let { stringResource(it) },
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        state.sheetError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun DispatchFifoSheet(
    state: PartDetailUiState,
    onDismiss: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_batch_dispatch_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !state.isSubmitting,
                loading = state.isSubmitting
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isSubmitting
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_quantity_label),
            value = state.batchQuantity,
            onValueChange = onQuantityChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.batchQtyError != null,
            errorText = state.batchQtyError?.let { stringResource(it) },
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        state.sheetError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestoreBatchSheet(
    state: PartDetailUiState,
    onDismiss: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onSelectBatch: (String?) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_batch_restore_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !state.isSubmitting,
                loading = state.isSubmitting
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isSubmitting
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_quantity_label),
            value = state.batchQuantity,
            onValueChange = onQuantityChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.batchQtyError != null,
            errorText = state.batchQtyError?.let { stringResource(it) },
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (state.batches.isNotEmpty()) {
            Text(
                text = stringResource(R.string.inv_seg2_batch_select_label),
                style = MaterialTheme.typography.titleSmall,
                color = AtelierTheme.colors.inkSecondary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = state.selectedBatchId == null,
                    onClick = { onSelectBatch(null) },
                    label = { Text(stringResource(R.string.inv_seg2_batch_select_auto)) },
                    shape = ControlShape,
                    enabled = !state.isSubmitting
                )
                state.batches.forEach { batch ->
                    FilterChip(
                        selected = state.selectedBatchId == batch.id,
                        onClick = { onSelectBatch(batch.id) },
                        label = { Text(stringResource(R.string.batch_number, batch.batchNumber)) },
                        shape = ControlShape,
                        enabled = !state.isSubmitting
                    )
                }
            }
        }
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_reason_label),
            value = state.restoreReason,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting
        )
        state.sheetError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun StockCalibrationCard(
    part: Part,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val stock = part.totalStock.toFloat().coerceAtLeast(0f)
    val minimum = part.minimumStock.toFloat().coerceAtLeast(0f)
    val scaleMax = maxOf(minimum * 2f, stock * 1.15f, 1f)
    val progress = (stock / scaleMax).coerceIn(0f, 1f)
    val markerAt = if (minimum > 0f) (minimum / scaleMax).coerceIn(0.08f, 0.92f) else null

    val barColor = when {
        part.isOutOfStock -> colors.badMark
        part.isLowStock -> colors.warnMark
        else -> colors.okMark
    }
    val statusTextRes = when {
        part.isOutOfStock -> R.string.premium_part_stock_out
        part.isLowStock -> R.string.premium_part_stock_low
        else -> R.string.premium_part_stock_healthy
    }
    val statusColor = when {
        part.isOutOfStock -> colors.bad
        part.isLowStock -> colors.warn
        else -> colors.ok
    }

    AtelierSurface(
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.premium_part_stock_level),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.premium_part_stock_ratio,
                        part.totalStock.formatQuantity(),
                        part.minimumStock.formatQuantity()
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AtelierMetricBar(
                progress = progress,
                color = barColor,
                height = 8.dp,
                markerAt = markerAt
            )
            Text(
                text = stringResource(statusTextRes),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
    }
}

@Composable
private fun Fact(labelRes: Int, value: String, showDivider: Boolean = true) {
    AtelierListRow(
        title = stringResource(labelRes),
        showDivider = showDivider,
        trailing = {
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    )
}

@Composable
private fun BatchRow(batch: Batch, isNext: Boolean) {
    val arrival = batch.arrivalDate?.let { stringResource(R.string.batch_arrival, it.formatLimaDate()) }
    val cost = batch.unitCost?.let { stringResource(R.string.batch_unit_cost, it.format()) }
    AtelierSurface(
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        AtelierListRow(
            title = stringResource(R.string.batch_number, batch.batchNumber),
            subtitle = listOfNotNull(
                stringResource(R.string.batch_remaining, batch.remainingQty.formatQuantity()),
                arrival,
                cost
            ).joinToString(" · "),
            showDivider = false,
            leading = if (isNext) {
                { AtelierLiveBeacon(color = AtelierTheme.colors.info) }
            } else null,
            trailing = {
                when {
                    isNext -> AtelierChip(text = stringResource(R.string.batch_next), kind = ChipKind.Info)
                    batch.isActive -> AtelierChip(text = stringResource(R.string.batch_waiting), kind = ChipKind.Ok)
                    else -> AtelierChip(text = stringResource(R.string.batch_depleted), kind = ChipKind.Idle)
                }
            }
        )
    }
}

private val previewPart = Part(
    id = "1", name = "Pastillas de freno delanteras", sku = "BRK-001", category = ItemCategory.BRAKES,
    basePrice = Money(BigDecimal("89.90"), Money.PEN), totalStock = BigDecimal("14"), minimumStock = BigDecimal("5"),
    unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
)

private fun previewBatch(id: String, number: String, remaining: String, month: Int) = Batch(
    id = id, itemId = "1", batchNumber = number, remainingQty = BigDecimal(remaining),
    unitCost = Money(BigDecimal("52.00"), Money.PEN), arrivalDate = Instant.parse("2026-0$month-10T12:00:00Z")
)

private val previewState = PartDetailUiState(
    part = previewPart, isLoading = false, canReadBatches = true, canManageParts = true,
    canReceiveBatch = true, canDispatchFifo = true, canRestoreBatch = true,
    batches = listOf(
        previewBatch("b1", "L-0001", "0", 3),
        previewBatch("b2", "L-0002", "6", 5),
        previewBatch("b3", "L-0003", "8", 7)
    )
)

@Composable
private fun PartDetailPreviewHost(state: PartDetailUiState) = PartDetailContent(
    state = state, onBack = {}, onRefresh = {}, onOpenEditPart = {}, onOpenReceiveBatch = {},
    onOpenDispatchFifo = {}, onOpenRestoreBatch = {}, onDismissSheet = {}, onEditNameChange = {},
    onEditCategoryChange = {}, onEditBasePriceChange = {}, onEditMinStockChange = {}, onEditStatusChange = {},
    onConfirmEditPart = {}, onBatchNumberChange = {}, onBatchQuantityChange = {}, onBatchUnitCostChange = {},
    onSelectedBatchIdChange = {}, onRestoreReasonChange = {}, onConfirmReceiveBatch = {},
    onConfirmDispatchFifo = {}, onConfirmRestoreBatch = {}
)

@Preview(name = "Repuesto · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PartDetailPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    PartDetailPreviewHost(previewState)
}

@Preview(name = "Repuesto · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PartDetailPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PartDetailPreviewHost(previewState)
}

@Preview(name = "Repuesto · sin permiso de lotes", widthDp = 390, heightDp = 844)
@Composable
private fun PartDetailPreviewNoPermission() = AtelierWorkshopTheme(darkTheme = false) {
    PartDetailPreviewHost(previewState.copy(canReadBatches = false, batches = emptyList()))
}
