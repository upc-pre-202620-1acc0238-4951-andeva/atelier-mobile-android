package com.andeva.atelier.mobile.features.inventory.presentation.orders

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
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
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLine
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.presentation.chipKind
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import java.io.File
import java.math.BigDecimal
import java.time.Instant

/**
 * US34 E2: orden de compra con ciclo completo (`DRAFT` → agregar líneas → `ISSUED` / `CANCELED` → `RECEIVED` con
 * comprobante fotográfico, H15). Requiere `PURCHASE_ORDERS_READ`.
 */
@Composable
fun PurchaseOrderDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.hapticEvent) {
        if (state.hapticEvent > 0) {
            if (state.hapticIsError) haptics.reject() else haptics.confirm()
        }
    }

    PurchaseOrderDetailContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenAddLine = viewModel::onOpenAddLine,
        onDismissAddLine = viewModel::onDismissAddLine,
        onPartQueryChange = viewModel::onPartQueryChange,
        onSelectPart = viewModel::onSelectPart,
        onLineQuantityChange = viewModel::onLineQuantityChange,
        onLineUnitCostChange = viewModel::onLineUnitCostChange,
        onConfirmAddLine = viewModel::onConfirmAddLine,
        onIssueOrder = viewModel::onIssueOrder,
        onOpenCancel = viewModel::onOpenCancel,
        onDismissCancel = viewModel::onDismissCancel,
        onCancelReasonChange = viewModel::onCancelReasonChange,
        onConfirmCancel = viewModel::onConfirmCancel,
        onOpenReceive = viewModel::onOpenReceive,
        onDismissReceive = viewModel::onDismissReceive,
        onReceiptNumberChange = viewModel::onReceiptNumberChange,
        onPhotoTaken = viewModel::onPhotoTaken,
        onConfirmReceive = viewModel::onConfirmReceive,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrderDetailContent(
    state: PurchaseOrderDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAddLine: () -> Unit,
    onDismissAddLine: () -> Unit,
    onPartQueryChange: (String) -> Unit,
    onSelectPart: (Part) -> Unit,
    onLineQuantityChange: (String) -> Unit,
    onLineUnitCostChange: (String) -> Unit,
    onConfirmAddLine: () -> Unit,
    onIssueOrder: () -> Unit,
    onOpenCancel: () -> Unit,
    onDismissCancel: () -> Unit,
    onCancelReasonChange: (String) -> Unit,
    onConfirmCancel: () -> Unit,
    onOpenReceive: () -> Unit,
    onDismissReceive: () -> Unit,
    onReceiptNumberChange: (String) -> Unit,
    onPhotoTaken: (File) -> Unit,
    onConfirmReceive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val order = state.order
    val delayedBones = rememberDelayedLoading(order == null && state.isLoading)
    val showSkeleton = order == null && state.isLoading && (LocalInspectionMode.current || delayedBones)

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canRead && state.canReceive && order?.canBeReceived == true) {
                AtelierFab(
                    text = stringResource(R.string.inventory_po_receive),
                    icon = Icons.Outlined.Inventory2,
                    onClick = onOpenReceive
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AtelierFichaPanel(
                onBack = onBack,
                title = order?.orderNumber?.let { stringResource(R.string.inventory_po_number, it) }
                    ?: stringResource(R.string.inventory_po_detail_title),
                subtitle = order?.totalCost?.format(),
                status = order?.let { { AtelierChip(text = stringResource(it.status.labelRes()), kind = it.status.chipKind()) } }
            )
            if (!state.canRead) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.inventory_po_no_permission),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            AtelierRefreshBar(visible = state.isLoading && order != null)
            PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (state.justReceived) {
                        item(key = "received") {
                            AtelierBanner(
                                message = stringResource(R.string.inventory_po_received_ok),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    state.actionBanner?.let { msg ->
                        item(key = "action-banner") {
                            AtelierBanner(
                                message = msg.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    state.actionError?.let { err ->
                        item(key = "action-error") {
                            AtelierBanner(
                                message = err.asString(),
                                icon = Icons.Outlined.ErrorOutline,
                                kind = BannerKind.Bad,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    val error = state.error
                    when {
                        showSkeleton -> items(3, key = { "bone-$it" }) {
                            AtelierListRow(title = "Filtro de aceite", subtitle = "4 × S/ 52.00", loading = true)
                        }
                        order == null && !state.isOnline -> item(key = "offline") {
                            AtelierOfflineState(onRetry = onRefresh, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        order == null && error != null -> item(key = "error") {
                            AtelierErrorState(
                                message = error.asString(),
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        order != null -> orderBody(
                            order = order,
                            state = state,
                            onOpenAddLine = onOpenAddLine,
                            onIssueOrder = onIssueOrder,
                            onOpenCancel = onOpenCancel
                        )
                    }
                }
            }
        }
    }

    if (state.showAddLineSheet) {
        AddLineSheet(
            state = state,
            onDismiss = onDismissAddLine,
            onPartQueryChange = onPartQueryChange,
            onSelectPart = onSelectPart,
            onQuantityChange = onLineQuantityChange,
            onUnitCostChange = onLineUnitCostChange,
            onConfirm = onConfirmAddLine
        )
    }

    if (state.showCancelSheet) {
        CancelOrderSheet(
            state = state,
            onDismiss = onDismissCancel,
            onReasonChange = onCancelReasonChange,
            onConfirm = onConfirmCancel
        )
    }

    if (state.showReceiveSheet) {
        ReceiveSheet(
            state = state,
            onDismiss = onDismissReceive,
            onReceiptNumberChange = onReceiptNumberChange,
            onPhotoTaken = onPhotoTaken,
            onConfirm = onConfirmReceive
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.orderBody(
    order: PurchaseOrder,
    state: PurchaseOrderDetailUiState,
    onOpenAddLine: () -> Unit,
    onIssueOrder: () -> Unit,
    onOpenCancel: () -> Unit
) {
    item(key = "lines-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.inventory_po_lines_header),
            count = order.lines.size,
            modifier = Modifier.padding(horizontal = 16.dp),
            action = if (order.isDraft && state.canCreate) {
                {
                    AtelierButton(
                        text = stringResource(R.string.inv_seg2_po_add_line_btn),
                        onClick = onOpenAddLine,
                        style = AtelierButtonStyle.Text,
                        icon = Icons.Outlined.Add
                    )
                }
            } else null
        )
    }
    if (order.lines.isEmpty()) {
        item(key = "lines-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.inv_seg2_po_issue_hint),
                actionLabel = if (order.isDraft && state.canCreate) stringResource(R.string.inv_seg2_po_add_line_btn) else null,
                onAction = if (order.isDraft && state.canCreate) onOpenAddLine else null,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    } else {
        item(key = "lines-card") {
            AtelierSurface(
                tier = SurfaceTier.Card,
                shape = CardLargeShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            ) {
                Column {
                    order.lines.forEachIndexed { index, line ->
                        AtelierListRow(
                            title = state.partNames[line.itemId] ?: stringResource(R.string.inventory_po_line_unknown),
                            subtitle = line.unitCost?.let {
                                stringResource(R.string.inventory_po_line_subtitle, line.quantity.formatQuantity(), it.format())
                            } ?: stringResource(R.string.inventory_po_line_quantity, line.quantity.formatQuantity()),
                            showDivider = index < order.lines.lastIndex,
                            trailing = {
                                line.totalCost?.let {
                                    Text(
                                        text = it.format(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    if (state.canCreate && (order.isDraft || order.canBeCanceled)) {
        item(key = "po-actions") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.isOnline) AtelierRequiresConnection()
                if (order.isDraft) {
                    if (order.lines.isEmpty()) {
                        Text(
                            text = stringResource(R.string.inv_seg2_po_issue_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtelierTheme.colors.inkSecondary
                        )
                    }
                    AtelierButton(
                        text = stringResource(R.string.inv_seg2_po_issue_btn),
                        onClick = onIssueOrder,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = order.lines.isNotEmpty() && state.isOnline && !state.isMutating,
                        loading = state.isMutating
                    )
                }
                if (order.canBeCanceled) {
                    AtelierButton(
                        text = stringResource(R.string.inv_seg2_po_cancel_btn),
                        onClick = onOpenCancel,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Destructive,
                        enabled = state.isOnline && !state.isMutating
                    )
                }
            }
        }
    }
    if (order.receivedAt != null || order.receiptNumber != null) {
        item(key = "receipt-header") {
            AtelierSectionHeader(
                title = stringResource(R.string.inventory_po_receipt_header),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item(key = "receipt-card") {
            AtelierSurface(
                tier = SurfaceTier.Card,
                shape = CardLargeShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            ) {
                Column {
                    order.receiptNumber?.let { number ->
                        AtelierListRow(
                            title = stringResource(R.string.inventory_po_receipt_number),
                            showDivider = order.receivedAt != null,
                            trailing = { Text(text = number, style = MaterialTheme.typography.titleMedium) }
                        )
                    }
                    order.receivedAt?.let { at ->
                        AtelierListRow(
                            title = stringResource(R.string.inventory_po_receipt_date),
                            showDivider = false,
                            trailing = { Text(text = at.formatLimaDate(), style = MaterialTheme.typography.titleMedium) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddLineSheet(
    state: PurchaseOrderDetailUiState,
    onDismiss: () -> Unit,
    onPartQueryChange: (String) -> Unit,
    onSelectPart: (Part) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitCostChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_po_add_line_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_po_add_line_btn),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && state.selectedPartId != null && !state.isAddingLine,
                loading = state.isAddingLine
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isAddingLine
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_po_select_part),
            value = state.partQuery,
            onValueChange = onPartQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.Search,
            placeholder = stringResource(R.string.inv_seg2_po_part_search_hint),
            enabled = !state.isAddingLine
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.filteredCatalogParts.forEach { part ->
                PurchaseOrderPartChip(
                    part = part,
                    selected = state.selectedPartId == part.id,
                    enabled = !state.isAddingLine,
                    onClick = { onSelectPart(part) }
                )
            }
        }
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_quantity_label),
            value = state.lineQuantity,
            onValueChange = onQuantityChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.lineQtyError != null,
            errorText = state.lineQtyError?.let { stringResource(it) },
            enabled = !state.isAddingLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_batch_unit_cost_label),
            value = state.lineUnitCost,
            onValueChange = onUnitCostChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.lineCostError != null,
            errorText = state.lineCostError?.let { stringResource(it) },
            enabled = !state.isAddingLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        state.addLineError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun CancelOrderSheet(
    state: PurchaseOrderDetailUiState,
    onDismiss: () -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_po_cancel_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_po_cancel_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.DestructiveFilled,
                enabled = state.isOnline && !state.isCanceling,
                loading = state.isCanceling
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isCanceling
            )
        }
    ) {
        Text(
            text = stringResource(R.string.inv_seg2_po_cancel_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_po_cancel_reason),
            value = state.cancelReason,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isCanceling
        )
        state.cancelError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun ReceiveSheet(
    state: PurchaseOrderDetailUiState,
    onDismiss: () -> Unit,
    onReceiptNumberChange: (String) -> Unit,
    onPhotoTaken: (File) -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<File?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pending?.takeIf { saved }?.let(onPhotoTaken)
    }
    val launchCamera = {
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        val file = File(dir, "receipt-${System.currentTimeMillis()}.jpg")
        pending = file
        takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }
    val onTakePhoto = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else askCamera.launch(Manifest.permission.CAMERA)
    }

    AtelierBottomSheet(
        title = stringResource(R.string.inventory_po_receive_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inventory_po_receive_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canConfirmReceive || state.isReceiving,
                loading = state.isReceiving
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isReceiving
            )
        }
    ) {
        Text(
            text = stringResource(R.string.inventory_po_receive_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AtelierTextField(
            label = stringResource(R.string.inventory_po_receipt_number),
            value = state.receiptNumber,
            onValueChange = onReceiptNumberChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isReceiving
        )
        if (state.photo != null) {
            AtelierChip(text = stringResource(R.string.inventory_po_photo_attached), kind = ChipKind.Ok)
        }
        AtelierButton(
            text = stringResource(if (state.photo == null) R.string.inventory_po_photo_take else R.string.inventory_po_photo_retake),
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Outlined,
            icon = Icons.Outlined.PhotoCamera,
            enabled = !state.isReceiving
        )
        state.receiveError?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

private val previewOrder = PurchaseOrder(
    id = "1", orderNumber = "OC-0042", supplierId = "s1", branchId = "b1", status = PurchaseOrderStatus.ISSUED,
    totalCost = Money(BigDecimal("1250.00"), Money.PEN), receiptNumber = null, receiptImageUrl = null, receivedAt = null,
    lines = listOf(
        PurchaseOrderLine("l1", "p1", BigDecimal("10"), Money(BigDecimal("52.00"), Money.PEN), Money(BigDecimal("520.00"), Money.PEN)),
        PurchaseOrderLine("l2", "p2", BigDecimal("20"), Money(BigDecimal("36.50"), Money.PEN), Money(BigDecimal("730.00"), Money.PEN))
    )
)

private val previewState = PurchaseOrderDetailUiState(
    order = previewOrder,
    partNames = mapOf("p1" to "Pastillas de freno delanteras", "p2" to "Filtro de aceite Bosch"),
    isLoading = false,
    canCreate = true,
    canReceive = true
)

@Composable
private fun PreviewContent(state: PurchaseOrderDetailUiState) = PurchaseOrderDetailContent(
    state = state, onBack = {}, onRefresh = {}, onOpenAddLine = {}, onDismissAddLine = {},
    onPartQueryChange = {}, onSelectPart = {}, onLineQuantityChange = {}, onLineUnitCostChange = {},
    onConfirmAddLine = {}, onIssueOrder = {}, onOpenCancel = {}, onDismissCancel = {},
    onCancelReasonChange = {}, onConfirmCancel = {}, onOpenReceive = {}, onDismissReceive = {},
    onReceiptNumberChange = {}, onPhotoTaken = {}, onConfirmReceive = {}
)

@Preview(name = "Orden de compra · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PurchaseOrderDetailPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Orden de compra · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseOrderDetailPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Orden de compra · borrador", widthDp = 390, heightDp = 844)
@Composable
private fun PurchaseOrderDetailPreviewDraft() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(order = previewOrder.copy(status = PurchaseOrderStatus.DRAFT)))
}
