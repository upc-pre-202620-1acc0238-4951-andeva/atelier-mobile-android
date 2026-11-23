package com.andeva.atelier.mobile.features.inventory.presentation.orders

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLine
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.presentation.chipKind
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import java.io.File
import java.math.BigDecimal
import java.time.Instant

/**
 * US34 E2: orden de compra y su recepción. Recibir exige `BATCHES_RECEIVE`, red, el número y la foto del
 * comprobante; se confirma en una hoja inferior. Requiere `PURCHASE_ORDERS_READ`.
 */
@Composable
fun PurchaseOrderDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PurchaseOrderDetailContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
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
    onOpenReceive: () -> Unit,
    onDismissReceive: () -> Unit,
    onReceiptNumberChange: (String) -> Unit,
    onPhotoTaken: (File) -> Unit,
    onConfirmReceive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val order = state.order
    Scaffold(
        modifier = modifier.safeDrawingPadding(),
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
                    val error = state.error
                    when {
                        order == null && state.isLoading -> items(3, key = { "bone-$it" }) {
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
                        order != null -> orderBody(order, state)
                    }
                }
            }
        }
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
    state: PurchaseOrderDetailUiState
) {
    item(key = "lines-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.inventory_po_lines_header),
            count = order.lines.size,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    items(order.lines, key = { it.id }) { line ->
        AtelierListRow(
            title = state.partNames[line.itemId] ?: stringResource(R.string.inventory_po_line_unknown),
            subtitle = line.unitCost?.let {
                stringResource(R.string.inventory_po_line_subtitle, line.quantity.formatQuantity(), it.format())
            } ?: stringResource(R.string.inventory_po_line_quantity, line.quantity.formatQuantity()),
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
    if (order.receivedAt != null || order.receiptNumber != null) {
        item(key = "receipt-header") {
            AtelierSectionHeader(
                title = stringResource(R.string.inventory_po_receipt_header),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        order.receiptNumber?.let { number ->
            item(key = "receipt-number") {
                AtelierListRow(
                    title = stringResource(R.string.inventory_po_receipt_number),
                    trailing = { Text(text = number, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }
        order.receivedAt?.let { at ->
            item(key = "receipt-date") {
                AtelierListRow(
                    title = stringResource(R.string.inventory_po_receipt_date),
                    trailing = { Text(text = at.formatLimaDate(), style = MaterialTheme.typography.titleMedium) }
                )
            }
        }
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
    canReceive = true
)

@Composable
private fun PreviewContent(state: PurchaseOrderDetailUiState) = PurchaseOrderDetailContent(
    state, onBack = {}, onRefresh = {}, onOpenReceive = {}, onDismissReceive = {}, onReceiptNumberChange = {},
    onPhotoTaken = {}, onConfirmReceive = {}
)

@Preview(name = "Orden de compra · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PurchaseOrderDetailPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Orden de compra · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseOrderDetailPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Orden de compra · recibida", widthDp = 390, heightDp = 844)
@Composable
private fun PurchaseOrderDetailPreviewReceived() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(
        previewState.copy(
            order = previewOrder.copy(
                status = PurchaseOrderStatus.RECEIVED, receiptNumber = "F001-004512",
                receivedAt = Instant.parse("2026-11-12T15:00:00Z")
            ),
            justReceived = true
        )
    )
}
