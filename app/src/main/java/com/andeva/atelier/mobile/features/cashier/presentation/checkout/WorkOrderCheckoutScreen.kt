package com.andeva.atelier.mobile.features.cashier.presentation.checkout

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierAdaptiveRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSprings
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.component.rememberReducedMotion
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlue
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.Payment
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLineDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherSummary
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.AmountRow
import com.andeva.atelier.mobile.features.cashier.presentation.chip
import com.andeva.atelier.mobile.features.cashier.presentation.formatAmount
import com.andeva.atelier.mobile.features.cashier.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.cashier.presentation.formatVoucherCode
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.math.BigDecimal
import java.time.Instant

/** Acciones de la pantalla, para que [WorkOrderCheckoutContent] no conozca al ViewModel. */
internal class CheckoutCallbacks(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onPrepareVoucher: () -> Unit,
    val onOpenPayment: () -> Unit,
    val onMarkPaid: () -> Unit,
    val onOpenExitPass: () -> Unit,
    val onShareExitPass: () -> Unit,
    val onOpenDeliver: () -> Unit,
    val onDismissSheet: () -> Unit,
    val onIssueType: (VoucherType) -> Unit,
    val onIssueDocumentType: (FiscalDocumentType) -> Unit,
    val onIssueTaxId: (String) -> Unit,
    val onIssueName: (String) -> Unit,
    val onIssueAddress: (String) -> Unit,
    val onIssueLineName: (Int, String) -> Unit,
    val onConfirmIssue: () -> Unit,
    val onPaymentAmount: (String) -> Unit,
    val onPaymentMethod: (PaymentMethod) -> Unit,
    val onPaymentReference: (String) -> Unit,
    val onConfirmPayment: () -> Unit,
    val onConfirmDeliver: () -> Unit,
    val onOpenVoid: (VoucherView) -> Unit,
    val onVoidReason: (String) -> Unit,
    val onConfirmVoid: () -> Unit,
    val onOpenCreditNote: (VoucherView) -> Unit,
    val onCreditNoteReason: (String) -> Unit,
    val onCreditNoteAmount: (String) -> Unit,
    val onConfirmCreditNote: () -> Unit,
    val onOpenVoucherPdf: (VoucherView) -> Unit = {},
    val onShareVoucherPdf: (VoucherView) -> Unit = {},
    val onShareVoucherXml: (VoucherView) -> Unit = {},
    val onShareVoucherCdr: (VoucherView) -> Unit = {},
    val onRefreshVoucherSunat: (VoucherView) -> Unit = {}
)

/**
 * "Cobrar y entregar": comprobante, cobros, pase de salida y entrega de una orden, en una sola pantalla con los
 * pasos a la vista. Emitir, cobrar y entregar son irreversibles: se confirman en una hoja inferior con la placa y el
 * monto. Todo es en línea; sin red las acciones se deshabilitan.
 */
@Composable
fun WorkOrderCheckoutScreen(
    onBack: () -> Unit,
    viewModel: WorkOrderCheckoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAtelierHaptics()
    val chooserTitle = stringResource(R.string.cashier_exit_pass_chooser)
    LaunchedEffect(state.exitPass) {
        val file = state.exitPass ?: return@LaunchedEffect
        val opened = when (state.exitPassActionMode) {
            CheckoutDocumentActionMode.OPEN -> DocumentIntents.openPdf(context, file, chooserTitle)
            CheckoutDocumentActionMode.SHARE -> DocumentIntents.sharePdf(context, file, chooserTitle)
        }
        if (opened) viewModel.onExitPassHandled() else viewModel.onExitPassOpenFailed()
    }
    LaunchedEffect(state.pendingDocument) {
        val doc = state.pendingDocument ?: return@LaunchedEffect
        val opened = when (doc.mode) {
            CheckoutDocumentActionMode.OPEN -> DocumentIntents.openFile(context, doc.file, doc.mimeType, chooserTitle)
            CheckoutDocumentActionMode.SHARE -> DocumentIntents.shareFile(context, doc.file, doc.mimeType, chooserTitle)
        }
        if (opened) viewModel.onDocumentHandled() else viewModel.onDocumentOpenFailed()
    }
    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.actionError, state.sheetError) {
        if (state.actionError != null || state.sheetError != null) haptics.reject()
    }
    WorkOrderCheckoutContent(
        state = state,
        callbacks = CheckoutCallbacks(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onPrepareVoucher = viewModel::onPrepareVoucher,
            onOpenPayment = viewModel::onOpenPayment,
            onMarkPaid = viewModel::onMarkPaid,
            onOpenExitPass = viewModel::onOpenExitPass,
            onShareExitPass = viewModel::onShareExitPass,
            onOpenDeliver = viewModel::onOpenDeliver,
            onDismissSheet = viewModel::onDismissSheet,
            onIssueType = viewModel::onIssueType,
            onIssueDocumentType = viewModel::onIssueDocumentType,
            onIssueTaxId = viewModel::onIssueTaxId,
            onIssueName = viewModel::onIssueName,
            onIssueAddress = viewModel::onIssueAddress,
            onIssueLineName = viewModel::onIssueLineName,
            onConfirmIssue = viewModel::onConfirmIssue,
            onPaymentAmount = viewModel::onPaymentAmount,
            onPaymentMethod = viewModel::onPaymentMethod,
            onPaymentReference = viewModel::onPaymentReference,
            onConfirmPayment = viewModel::onConfirmPayment,
            onConfirmDeliver = viewModel::onConfirmDeliver,
            onOpenVoid = viewModel::onOpenVoid,
            onVoidReason = viewModel::onVoidReason,
            onConfirmVoid = viewModel::onConfirmVoid,
            onOpenCreditNote = viewModel::onOpenCreditNote,
            onCreditNoteReason = viewModel::onCreditNoteReason,
            onCreditNoteAmount = viewModel::onCreditNoteAmount,
            onConfirmCreditNote = viewModel::onConfirmCreditNote,
            onOpenVoucherPdf = viewModel::onOpenVoucherPdf,
            onShareVoucherPdf = viewModel::onShareVoucherPdf,
            onShareVoucherXml = viewModel::onShareVoucherXml,
            onShareVoucherCdr = viewModel::onShareVoucherCdr,
            onRefreshVoucherSunat = viewModel::onRefreshVoucherSunat
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkOrderCheckoutContent(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.hasLoaded,
            onRefresh = callbacks.onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                item(key = "ficha") { CheckoutHeader(state, callbacks.onBack) }
                item(key = "refresh") { AtelierRefreshBar(visible = state.isRefreshing && state.hasLoaded) }
                if (!state.hasAnyAccess) {
                    item(key = "no-access") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.cashier_checkout_no_access),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item(key = "progress-rail") {
                        CheckoutProgressRail(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xs)
                        )
                    }
                    messages(state, callbacks)
                    voucherStep(state, showSkeleton, callbacks)
                    paymentsStep(state, showSkeleton, callbacks)
                    exitPassStep(state, callbacks)
                    deliveryStep(state, callbacks)
                }
            }
        }
    }
    CheckoutSheets(state, callbacks)
}

@Composable
private fun CheckoutHeader(state: WorkOrderCheckoutUiState, onBack: () -> Unit) {
    val order = state.order
    val context = stringResource(R.string.cashier_checkout_title)
    val detail = listOfNotNull(
        order?.internalNumber?.let { stringResource(R.string.cashier_order_number, it) },
        order?.customerName
    ).joinToString(" · ")
    AtelierFichaPanel(
        onBack = onBack,
        title = order?.totalAmount?.format() ?: context,
        subtitle = if (order?.totalAmount != null) listOf(context, detail).filter { it.isNotBlank() }.joinToString(" · ") else detail.ifBlank { null },
        identity = order?.plate?.let { plate -> { AtelierPlate(plate, size = PlateSize.L) } },
        status = {
            val status = order?.status
            when {
                state.delivered -> AtelierChip(stringResource(R.string.cashier_delivered), kind = ChipKind.Ok)
                status != null -> {
                    val (label, kind) = status.headerChip()
                    AtelierChip(stringResource(label), kind = kind)
                }
            }
        }
    )
}

private fun WorkOrderStatus.headerChip(): Pair<Int, ChipKind> = when (this) {
    WorkOrderStatus.DRAFT -> R.string.order_status_draft to ChipKind.Idle
    WorkOrderStatus.IN_PROGRESS -> R.string.order_status_in_progress to ChipKind.Info
    WorkOrderStatus.COMPLETED -> R.string.order_status_completed to ChipKind.Info
    WorkOrderStatus.PAID -> R.string.order_status_paid to ChipKind.Ok
    WorkOrderStatus.CANCELED -> R.string.order_status_canceled to ChipKind.Bad
}

private fun LazyListScope.messages(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.isOnline) {
        item(key = "offline") {
            AtelierBanner(
                message = stringResource(R.string.today_offline_cache),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Info,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
        }
    }
    state.notice?.let { notice ->
        item(key = "notice") {
            AtelierBanner(
                message = notice.asString(),
                icon = Icons.Outlined.CheckCircle,
                kind = BannerKind.Ok,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
        }
    }
    val failure = state.actionError ?: state.error.takeIf { state.order != null || state.vouchers.isNotEmpty() }
    failure?.let { error ->
        item(key = "failure") {
            AtelierBanner(
                message = error.asString(),
                icon = Icons.Outlined.ErrorOutline,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
        }
    }
    if (state.error != null && state.order == null && state.vouchers.isEmpty() && state.actionError == null) {
        item(key = "load-error") {
            if (!state.isOnline) {
                AtelierOfflineState(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onRetry = callbacks.onRefresh
                )
            } else {
                AtelierErrorState(
                    message = state.error.asString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onRetry = callbacks.onRefresh
                )
            }
        }
    }
}

// region Riel y pasos

private enum class RailStageState { COMPLETED, ACTIVE, PENDING }

@Composable
private fun CheckoutProgressRail(
    state: WorkOrderCheckoutUiState,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = rememberReducedMotion()

    val voucherDone = state.activeVoucher != null
    val paymentDone = state.isOrderPaid || state.delivered
    val passDone = state.delivered
    val deliveryDone = state.delivered

    val activeIndex: Int? = when {
        state.delivered -> null
        !voucherDone -> 0
        !paymentDone -> 1
        state.canIssueExitPass -> 2
        else -> 3
    }

    val stageStates = listOf(
        when {
            voucherDone -> RailStageState.COMPLETED
            activeIndex == 0 -> RailStageState.ACTIVE
            else -> RailStageState.PENDING
        },
        when {
            paymentDone -> RailStageState.COMPLETED
            activeIndex == 1 -> RailStageState.ACTIVE
            else -> RailStageState.PENDING
        },
        when {
            passDone -> RailStageState.COMPLETED
            activeIndex == 2 -> RailStageState.ACTIVE
            else -> RailStageState.PENDING
        },
        when {
            deliveryDone -> RailStageState.COMPLETED
            activeIndex == 3 -> RailStageState.ACTIVE
            else -> RailStageState.PENDING
        }
    )

    val completedSegments = when {
        deliveryDone -> 3f
        paymentDone -> 2f
        voucherDone -> 1f
        else -> 0f
    }
    val animatedSegments by animateFloatAsState(
        targetValue = completedSegments,
        animationSpec = if (reducedMotion) tween(0) else AtelierSprings.criticallyDamped(),
        label = "checkoutRailSegments"
    )

    val labels = listOf(
        stringResource(R.string.premium_checkout_rail_voucher),
        stringResource(R.string.premium_checkout_rail_payment),
        stringResource(R.string.premium_checkout_rail_pass),
        stringResource(R.string.premium_checkout_rail_delivery)
    )

    val statusText = if (activeIndex == null) {
        stringResource(R.string.premium_checkout_rail_done)
    } else {
        stringResource(R.string.premium_checkout_rail_step_of, activeIndex + 1)
    }

    AtelierSurface(
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cashier_checkout_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (activeIndex == null) colors.ok else colors.primaryText
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                val trackColor = scheme.outlineVariant
                val completedColor = colors.okMark

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    val count = 4
                    val firstCenterX = size.width * (0.5f / count)
                    val lastCenterX = size.width * ((count - 0.5f) / count)
                    val totalSpan = (lastCenterX - firstCenterX).coerceAtLeast(1f)
                    val strokePx = 2.5.dp.toPx()

                    drawLine(
                        color = trackColor,
                        start = Offset(firstCenterX, 0f),
                        end = Offset(lastCenterX, 0f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )

                    if (animatedSegments > 0f) {
                        val activeEndX = firstCenterX + totalSpan * (animatedSegments / (count - 1).toFloat()).coerceIn(0f, 1f)
                        drawLine(
                            color = completedColor,
                            start = Offset(firstCenterX, 0f),
                            end = Offset(activeEndX, 0f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    stageStates.forEachIndexed { index, stage ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (stage) {
                                    RailStageState.COMPLETED -> Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(colors.okMark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = colors.onPanel,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    RailStageState.ACTIVE -> AtelierLiveBeacon(
                                        color = AtelierBlue,
                                        size = 11.dp,
                                        pulsing = true
                                    )
                                    RailStageState.PENDING -> Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceRecessed)
                                            .border(2.dp, scheme.outlineVariant, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = labels[index],
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (stage == RailStageState.ACTIVE) FontWeight.Bold else FontWeight.Medium,
                                color = when (stage) {
                                    RailStageState.COMPLETED -> colors.ok
                                    RailStageState.ACTIVE -> scheme.onSurface
                                    RailStageState.PENDING -> colors.inkSecondary
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    number: Int,
    @androidx.annotation.StringRes label: Int,
    modifier: Modifier = Modifier,
    chip: (@Composable () -> Unit)? = null
) {
    AtelierSectionHeader(
        title = stringResource(R.string.cashier_step_title, number, stringResource(label)),
        modifier = modifier,
        action = chip
    )
}

@Composable
private fun StepHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary,
        modifier = modifier
    )
}

@Composable
private fun StepAction(
    text: String,
    onClick: () -> Unit,
    state: WorkOrderCheckoutUiState,
    action: CheckoutAction,
    modifier: Modifier = Modifier,
    style: AtelierButtonStyle = AtelierButtonStyle.Filled,
    enabled: Boolean = true
) {
    AtelierButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = style,
        enabled = enabled && state.isOnline && (state.busy == null || state.busy == action),
        loading = state.busy == action
    )
}

private fun LazyListScope.voucherStep(
    state: WorkOrderCheckoutUiState,
    showSkeleton: Boolean,
    callbacks: CheckoutCallbacks
) {
    if (!state.canReadInvoices && !state.canIssueVoucher) return
    val isActive = state.activeVoucher == null && !state.delivered
    item(key = "step-voucher-card") {
        AtelierSurface(
            tier = if (isActive) SurfaceTier.Elevated else SurfaceTier.Card,
            shape = CardLargeShape,
            contentPadding = PaddingValues(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StepHeader(1, R.string.cashier_step_voucher) {
                    state.activeVoucher?.let { voucher ->
                        val (label, kind) = voucher.status.chip()
                        AtelierChip(stringResource(label), kind = kind)
                    }
                }
                when {
                    showSkeleton -> repeat(2) { VoucherSkeletonRow() }
                    state.showSkeleton -> Unit
                    else -> {
                        if (state.hasDifferentialCascadeNote) {
                            AtelierBanner(
                                message = stringResource(R.string.cashier_checkout_h26_note),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info
                            )
                        }
                        if (state.hasRejectedVoucherWithoutActive) {
                            AtelierBanner(
                                message = stringResource(R.string.cashier_sunat_rejected_reissue_hint),
                                icon = Icons.Outlined.ErrorOutline,
                                kind = BannerKind.Bad
                            )
                        }
                        state.vouchers.forEachIndexed { index, voucher ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            VoucherBlock(
                                voucher = voucher,
                                showStatusChip = state.vouchers.size > 1 || voucher.id != state.activeVoucher?.id,
                                canManageVoucher = state.canIssueVoucher,
                                isOnline = state.isOnline,
                                busy = state.busy,
                                onCreditNote = { callbacks.onOpenCreditNote(voucher) },
                                onVoid = { callbacks.onOpenVoid(voucher) },
                                onOpenPdf = { callbacks.onOpenVoucherPdf(voucher) },
                                onSharePdf = { callbacks.onShareVoucherPdf(voucher) },
                                onShareXml = { callbacks.onShareVoucherXml(voucher) },
                                onShareCdr = { callbacks.onShareVoucherCdr(voucher) },
                                onRefreshSunat = { callbacks.onRefreshVoucherSunat(voucher) }
                            )
                        }
                        if (state.activeVoucher == null) {
                            when {
                                state.canIssueVoucher && state.isCompleted -> StepAction(
                                    text = stringResource(R.string.cashier_voucher_prepare),
                                    onClick = callbacks.onPrepareVoucher,
                                    state = state,
                                    action = CheckoutAction.Preparing
                                )
                                state.canIssueVoucher && state.order != null ->
                                    StepHint(stringResource(R.string.cashier_voucher_needs_completed))
                                state.vouchers.isEmpty() -> AtelierEmptyState(
                                    text = stringResource(R.string.cashier_voucher_empty)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoucherSkeletonRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Boleta B001-00000001",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.bone(loading = true, widthFraction = 0.65f)
        )
        Text(
            text = "Total S/ 0.00",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.bone(loading = true, widthFraction = 0.4f)
        )
    }
}

@Composable
private fun VoucherBlock(
    voucher: VoucherView,
    showStatusChip: Boolean = true,
    canManageVoucher: Boolean,
    isOnline: Boolean,
    busy: CheckoutAction?,
    onCreditNote: () -> Unit,
    onVoid: () -> Unit,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXml: () -> Unit,
    onShareCdr: () -> Unit,
    onRefreshSunat: () -> Unit
) {
    val name = stringResource(
        R.string.cashier_voucher_name,
        stringResource(voucher.type.labelRes()),
        formatVoucherCode(voucher.serie, voucher.number)
    )
    val (statusLabel, statusKind) = voucher.status.chip()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (showStatusChip) {
                AtelierChip(stringResource(statusLabel), kind = statusKind)
            }
        }
        if (!voucher.sunatResponseCode.isNullOrBlank() || !voucher.sunatDescription.isNullOrBlank()) {
            Text(
                text = stringResource(
                    R.string.cashier_vouchers_cdr_code,
                    voucher.sunatResponseCode.orEmpty().ifBlank { "—" },
                    voucher.sunatDescription.orEmpty().ifBlank { stringResource(statusLabel) }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        if (voucher.subtotal != null && voucher.igv != null) {
            AmountRow(stringResource(R.string.cashier_subtotal), formatAmount(voucher.subtotal, voucher.currency))
            AmountRow(stringResource(R.string.cashier_igv, 18), formatAmount(voucher.igv, voucher.currency))
        }
        AmountRow(stringResource(R.string.cashier_total), formatAmount(voucher.total, voucher.currency), emphasized = true)
        if (!voucher.pdfUrl.isNullOrBlank()) {
            AtelierAdaptiveRow(
                modifier = Modifier.padding(top = Spacing.xs),
                equalWidthInRow = true
            ) {
                AtelierButton(
                    text = stringResource(R.string.cashier_doc_open_pdf),
                    onClick = onOpenPdf,
                    style = AtelierButtonStyle.Tonal,
                    enabled = isOnline && busy == null,
                    loading = busy == CheckoutAction.DownloadingDocument
                )
                AtelierButton(
                    text = stringResource(R.string.cashier_doc_share_pdf),
                    onClick = onSharePdf,
                    style = AtelierButtonStyle.Outlined,
                    enabled = isOnline && busy == null
                )
            }
        }
        if (!voucher.xmlUrl.isNullOrBlank() || !voucher.cdrUrl.isNullOrBlank()) {
            AtelierAdaptiveRow(
                modifier = Modifier.padding(top = Spacing.xs),
                equalWidthInRow = true
            ) {
                if (!voucher.xmlUrl.isNullOrBlank()) {
                    AtelierButton(
                        text = stringResource(R.string.cashier_doc_share_xml),
                        onClick = onShareXml,
                        style = AtelierButtonStyle.Outlined,
                        enabled = isOnline && busy == null
                    )
                }
                if (!voucher.cdrUrl.isNullOrBlank()) {
                    AtelierButton(
                        text = stringResource(R.string.cashier_doc_share_cdr),
                        onClick = onShareCdr,
                        style = AtelierButtonStyle.Outlined,
                        enabled = isOnline && busy == null
                    )
                }
            }
        }
        if (voucher.status == VoucherStatus.ISSUED) {
            AtelierButton(
                text = stringResource(R.string.cashier_sunat_refresh_status),
                onClick = onRefreshSunat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
                style = AtelierButtonStyle.Tonal,
                enabled = isOnline && busy == null,
                loading = busy == CheckoutAction.RefreshingSunat
            )
        }
        if (canManageVoucher && voucher.canVoidOrCreditNote) {
            AtelierAdaptiveRow(
                modifier = Modifier.padding(top = Spacing.xs),
                equalWidthInRow = true
            ) {
                AtelierButton(
                    text = stringResource(R.string.cashier_nc_short),
                    onClick = onCreditNote,
                    style = AtelierButtonStyle.Outlined,
                    enabled = isOnline && busy == null,
                    loading = busy == CheckoutAction.IssuingCreditNote
                )
                AtelierButton(
                    text = stringResource(R.string.cashier_void_short),
                    onClick = onVoid,
                    style = AtelierButtonStyle.Destructive,
                    enabled = isOnline && busy == null,
                    loading = busy == CheckoutAction.Voiding
                )
            }
        }
    }
}

private fun LazyListScope.paymentsStep(
    state: WorkOrderCheckoutUiState,
    showSkeleton: Boolean,
    callbacks: CheckoutCallbacks
) {
    if (!state.canReadInvoices && !state.canRegisterPayment) return
    val voucher = state.activeVoucher
    val isActive = voucher != null && !state.isOrderPaid && !state.delivered
    item(key = "step-payments-card") {
        AtelierSurface(
            tier = if (isActive) SurfaceTier.Elevated else SurfaceTier.Card,
            shape = CardLargeShape,
            contentPadding = PaddingValues(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StepHeader(2, R.string.cashier_step_payments) {
                    voucher?.let {
                        val (label, kind) = it.paymentStatus.chip()
                        AtelierChip(stringResource(label), kind = kind)
                    }
                }
                if (voucher == null) {
                    if (!showSkeleton && !state.showSkeleton) {
                        StepHint(stringResource(R.string.cashier_payments_needs_voucher))
                    }
                } else {
                    Column {
                        AmountRow(stringResource(R.string.cashier_collected), formatAmount(voucher.paid, voucher.currency))
                        AmountRow(stringResource(R.string.cashier_balance), formatAmount(voucher.pending, voucher.currency), emphasized = true)
                    }
                    if (state.payments.isEmpty()) {
                        StepHint(stringResource(R.string.cashier_payments_empty))
                    } else {
                        Column {
                            state.payments.forEachIndexed { index, payment ->
                                PaymentRow(
                                    payment = payment,
                                    currency = voucher.currency,
                                    showDivider = index < state.payments.lastIndex
                                )
                            }
                        }
                    }
                    if (state.canRegisterPayment && voucher.pending.signum() > 0) {
                        StepAction(
                            text = stringResource(R.string.cashier_payment_register),
                            onClick = callbacks.onOpenPayment,
                            state = state,
                            action = CheckoutAction.Paying
                        )
                    }
                    if (state.canMarkPaid) {
                        StepAction(
                            text = stringResource(R.string.cashier_mark_paid),
                            onClick = callbacks.onMarkPaid,
                            state = state,
                            action = CheckoutAction.MarkingPaid
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, currency: String?, showDivider: Boolean = true) {
    val time = payment.paidAt?.formatLimaDateTime()
    val reference = payment.transactionReference?.takeIf { it.isNotBlank() }
    val subtitle = listOfNotNull(time, reference).joinToString(" · ").ifBlank { null }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(payment.method.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
            }
            Text(
                text = formatAmount(payment.amount.amount, payment.amount.currency.ifBlank { currency }),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private fun LazyListScope.exitPassStep(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.canIssueExitPass) return
    val isActive = state.isOrderPaid && !state.delivered
    item(key = "step-exit-pass-card") {
        AtelierSurface(
            tier = if (isActive) SurfaceTier.Elevated else SurfaceTier.Card,
            shape = CardLargeShape,
            contentPadding = PaddingValues(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StepHeader(3, R.string.cashier_step_exit_pass)
                if (!state.isOrderPaid) StepHint(stringResource(R.string.cashier_exit_pass_needs_paid))
                AtelierAdaptiveRow(equalWidthInRow = true) {
                    StepAction(
                        text = stringResource(R.string.cashier_exit_pass_open),
                        onClick = callbacks.onOpenExitPass,
                        state = state,
                        action = CheckoutAction.ExitPass,
                        style = AtelierButtonStyle.Tonal,
                        enabled = state.isOrderPaid
                    )
                    StepAction(
                        text = stringResource(R.string.cashier_exit_pass_share),
                        onClick = callbacks.onShareExitPass,
                        state = state,
                        action = CheckoutAction.ExitPass,
                        style = AtelierButtonStyle.Outlined,
                        enabled = state.isOrderPaid
                    )
                }
            }
        }
    }
}

private fun LazyListScope.deliveryStep(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.canDeliver) return
    val isActive = state.isOrderPaid && !state.delivered && !state.canIssueExitPass
    item(key = "step-delivery-card") {
        AtelierSurface(
            tier = if (isActive) SurfaceTier.Elevated else SurfaceTier.Card,
            shape = CardLargeShape,
            contentPadding = PaddingValues(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StepHeader(4, R.string.cashier_step_delivery) {
                    if (state.delivered) AtelierChip(stringResource(R.string.cashier_delivered), kind = ChipKind.Ok)
                }
                if (!state.isOrderPaid && !state.delivered) StepHint(stringResource(R.string.cashier_deliver_needs_paid))
                if (!state.delivered) {
                    StepAction(
                        text = stringResource(R.string.cashier_deliver_action),
                        onClick = callbacks.onOpenDeliver,
                        state = state,
                        action = CheckoutAction.Delivering,
                        style = if (state.isOrderPaid) AtelierButtonStyle.Filled else AtelierButtonStyle.Outlined,
                        enabled = state.isOrderPaid
                    )
                }
            }
        }
    }
}

// endregion

// region Previews

private fun soles(amount: String) = Money(BigDecimal(amount), Money.PEN)

private val previewOrder = WorkOrder(
    id = "wo-424",
    internalNumber = 424,
    branchId = "b1",
    vehicleId = "v1",
    customerId = "c1",
    currentBayId = null,
    bayName = null,
    status = WorkOrderStatus.COMPLETED,
    diagnosticSummary = null,
    mileageIn = 84_500,
    subtotal = soles("1016.95"),
    taxAmount = soles("183.05"),
    totalAmount = soles("1200.00"),
    plate = "BKR418",
    customerName = "Rosa Quispe Huamán"
)

private val previewVoucher = VoucherSummary(
    id = "vch-1",
    workOrderId = "wo-424",
    type = VoucherType.BOLETA,
    serie = "B001",
    number = 45,
    totalAmount = BigDecimal("1200.00"),
    paidAmount = BigDecimal("500.00"),
    pendingBalance = BigDecimal("700.00"),
    paymentStatus = VoucherPaymentStatus.PARTIAL,
    status = VoucherStatus.ACCEPTED_SUNAT,
    pdfUrl = null
)

private val previewPayments = listOf(
    Payment(
        id = "p1",
        voucherId = "vch-1",
        amount = soles("500.00"),
        method = PaymentMethod.DIGITAL_WALLET_YAPE,
        transactionReference = "YP-883201",
        status = "COMPLETED",
        paidAt = Instant.parse("2026-11-16T19:35:00Z")
    )
)

private val previewState = WorkOrderCheckoutUiState(
    workOrderId = "wo-424",
    order = previewOrder,
    cachedVouchers = listOf(previewVoucher),
    payments = previewPayments,
    hasLoaded = true,
    canReadInvoices = true,
    canIssueVoucher = true,
    canRegisterPayment = true,
    canIssueExitPass = true,
    canDeliver = true
)

private val previewDraft = VoucherDraft(
    workOrderId = "wo-424",
    customerId = "c1",
    type = VoucherType.BOLETA,
    customerInfo = CustomerFiscalInfo(FiscalDocumentType.DNI, "45871236", "Rosa Quispe Huamán", null),
    currency = "PEN",
    lines = listOf(
        VoucherLineDraft(VoucherItemType.SERVICE, "s1", "Cambio de pastillas de freno", BigDecimal.ONE, BigDecimal("708.00")),
        VoucherLineDraft(VoucherItemType.PRODUCT, "p1", "Product 1a2b3c4d-1111-2222-3333-444455556666", BigDecimal("2"), BigDecimal("246.00"))
    )
)

private val noCallbacks = CheckoutCallbacks(
    onBack = {}, onRefresh = {}, onPrepareVoucher = {}, onOpenPayment = {}, onMarkPaid = {},
    onOpenExitPass = {}, onShareExitPass = {},
    onOpenDeliver = {}, onDismissSheet = {}, onIssueType = {}, onIssueDocumentType = {}, onIssueTaxId = {},
    onIssueName = {}, onIssueAddress = {}, onIssueLineName = { _, _ -> }, onConfirmIssue = {}, onPaymentAmount = {},
    onPaymentMethod = {}, onPaymentReference = {}, onConfirmPayment = {}, onConfirmDeliver = {},
    onOpenVoid = {}, onVoidReason = {}, onConfirmVoid = {}, onOpenCreditNote = {},
    onCreditNoteReason = {}, onCreditNoteAmount = {}, onConfirmCreditNote = {}
)

@Composable
private fun PreviewContent(state: WorkOrderCheckoutUiState) {
    Surface(color = MaterialTheme.colorScheme.background) { WorkOrderCheckoutContent(state, noCallbacks) }
}

@Preview(name = "Cobrar y entregar · claro", widthDp = 390, heightDp = 844)
@Composable
private fun CheckoutPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Cobrar y entregar · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CheckoutPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Cobrar y entregar · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun CheckoutPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(order = previewOrder, cachedVouchers = emptyList(), payments = emptyList(), hasLoaded = false))
}

@Preview(name = "Cobrar y entregar · sin conexión", widthDp = 390, heightDp = 844)
@Composable
private fun CheckoutPreviewOffline() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(isOnline = false))
}

@Preview(name = "Emitir y cobrar · hojas", widthDp = 390, showBackground = true)
@Composable
private fun CheckoutSheetsPreview() = AtelierWorkshopTheme(darkTheme = false) {
    val form = IssueForm(
        base = previewDraft,
        type = VoucherType.BOLETA,
        documentType = FiscalDocumentType.DNI,
        taxId = "45871236",
        legalName = "Rosa Quispe Huamán",
        address = "",
        lines = previewDraft.lines
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IssueSheetContent(previewOrder, form, null, noCallbacks)
            PaymentSheetContent(previewOrder, PaymentForm(previewVoucher.toView(), "700.00"), null, noCallbacks)
        }
    }
}

// endregion
