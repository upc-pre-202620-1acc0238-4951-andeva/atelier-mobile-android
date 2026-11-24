package com.andeva.atelier.mobile.features.cashier.presentation.vouchers

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierAdaptiveRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.component.springPressScale
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLine
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.AmountRow
import com.andeva.atelier.mobile.features.cashier.presentation.CreditNoteForm
import com.andeva.atelier.mobile.features.cashier.presentation.CreditNoteSheetContent
import com.andeva.atelier.mobile.features.cashier.presentation.VoidForm
import com.andeva.atelier.mobile.features.cashier.presentation.VoidSheetContent
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.CheckoutDocumentActionMode
import com.andeva.atelier.mobile.features.cashier.presentation.chip
import com.andeva.atelier.mobile.features.cashier.presentation.formatAmount
import com.andeva.atelier.mobile.features.cashier.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.cashier.presentation.formatVoucherCode
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes
import java.math.BigDecimal
import java.time.Instant

private class VouchersCallbacks(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onFilterChange: (VoucherFilter) -> Unit,
    val onOpenVoucher: (Voucher) -> Unit,
    val onStartVoid: () -> Unit,
    val onVoidReason: (String) -> Unit,
    val onConfirmVoid: () -> Unit,
    val onStartCreditNote: () -> Unit,
    val onCreditNoteReason: (String) -> Unit,
    val onCreditNoteAmount: (String) -> Unit,
    val onCreditNoteSerie: (String) -> Unit = {},
    val onConfirmCreditNote: () -> Unit,
    val onOpenPdf: () -> Unit = {},
    val onSharePdf: () -> Unit = {},
    val onShareXml: () -> Unit = {},
    val onShareCdr: () -> Unit = {},
    val onRefreshSunat: () -> Unit = {},
    val onBackToDetail: () -> Unit,
    val onDismissSheet: () -> Unit
)

/**
 * Comprobantes SUNAT emitidos en la sede: boletas, facturas y notas de crédito.
 * Lectura con `invoicing:invoices:read`; anulación y nota de crédito con `invoicing:invoices:issue_sunat` en línea.
 */
@Composable
fun VouchersScreen(
    onBack: () -> Unit,
    viewModel: VouchersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAtelierHaptics()
    val chooserTitle = stringResource(R.string.cashier_vouchers_title)

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
    LaunchedEffect(state.sheetError, state.error) {
        if (state.sheetError != null || state.error != null) haptics.reject()
    }

    VouchersContent(
        state = state,
        callbacks = VouchersCallbacks(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onFilterChange = viewModel::onFilterChange,
            onOpenVoucher = viewModel::onOpenVoucher,
            onStartVoid = viewModel::onStartVoid,
            onVoidReason = viewModel::onVoidReason,
            onConfirmVoid = viewModel::onConfirmVoid,
            onStartCreditNote = viewModel::onStartCreditNote,
            onCreditNoteReason = viewModel::onCreditNoteReason,
            onCreditNoteAmount = viewModel::onCreditNoteAmount,
            onCreditNoteSerie = viewModel::onCreditNoteSerie,
            onConfirmCreditNote = viewModel::onConfirmCreditNote,
            onOpenPdf = viewModel::onOpenPdf,
            onSharePdf = viewModel::onSharePdf,
            onShareXml = viewModel::onShareXml,
            onShareCdr = viewModel::onShareCdr,
            onRefreshSunat = viewModel::onRefreshSunat,
            onBackToDetail = viewModel::onBackToDetail,
            onDismissSheet = viewModel::onDismissSheet
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VouchersContent(state: VouchersUiState, callbacks: VouchersCallbacks) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton && state.canRead)
    val visible = state.filteredVouchers

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.cashier_vouchers_title), onBack = callbacks.onBack) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.hasLoaded,
            onRefresh = callbacks.onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp + LocalFloatingBarInset.current)
            ) {
                item(key = "refresh") {
                    AtelierRefreshBar(visible = state.isRefreshing && state.hasLoaded)
                }
                if (!state.canRead) {
                    item(key = "no-access") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.cashier_vouchers_no_access),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    return@LazyColumn
                }
                if (!state.isOnline) {
                    item(key = "offline-banner") {
                        AtelierBanner(
                            message = stringResource(R.string.error_requires_connection),
                            icon = Icons.Outlined.Info,
                            kind = BannerKind.Info,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        )
                    }
                }
                state.notice?.let { notice ->
                    item(key = "notice") {
                        AtelierBanner(
                            message = notice.asString(),
                            icon = Icons.Outlined.CheckCircle,
                            kind = BannerKind.Ok,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        )
                    }
                }
                item(key = "filters") {
                    FilterRow(selected = state.filter, onSelect = callbacks.onFilterChange)
                }
                when {
                    showSkeleton -> items(5, key = { "bone-$it" }) {
                        VoucherRow(voucher = previewVouchers.first(), loading = true, onClick = {})
                    }
                    state.showSkeleton -> Unit
                    state.error != null && state.vouchers.isEmpty() -> item(key = "error") {
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
                    state.vouchers.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.cashier_vouchers_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    visible.isEmpty() -> item(key = "empty-filtered") {
                        AtelierEmptyState(
                            text = stringResource(R.string.cashier_vouchers_empty_filtered),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(visible, key = { it.id }) { voucher ->
                        VoucherRow(
                            voucher = voucher,
                            loading = false,
                            onClick = { callbacks.onOpenVoucher(voucher) }
                        )
                    }
                }
            }
        }
    }
    VouchersSheets(state, callbacks)
}

@Composable
private fun FilterRow(selected: VoucherFilter, onSelect: (VoucherFilter) -> Unit) {
    val labels = VoucherFilter.entries.associateWith { stringResource(it.labelRes()) }
    AtelierSegmentedControl(
        options = VoucherFilter.entries,
        selected = selected,
        onSelect = onSelect,
        label = { labels.getValue(it) },
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}

@Composable
private fun VoucherRow(
    voucher: Voucher,
    loading: Boolean,
    onClick: () -> Unit
) {
    val stacked = LocalDensity.current.fontScale >= 1.15f
    val interaction = remember { MutableInteractionSource() }
    val title = stringResource(
        R.string.cashier_voucher_name,
        stringResource(voucher.type.labelRes()),
        formatVoucherCode(voucher.serie, voucher.number)
    )
    val balanceLabel = if (voucher.isActiveSale && voucher.pendingBalance.amount.signum() > 0) {
        stringResource(R.string.cashier_vouchers_balance_short, voucher.pendingBalance.format())
    } else null
    val subtitle = listOfNotNull(
        voucher.customerInfo?.legalName?.takeIf { it.isNotBlank() },
        voucher.issuedAt?.formatLimaDateTime(),
        balanceLabel
    ).joinToString(" · ").ifBlank { null }
    val (statusLabel, statusKind) = voucher.status.chip()
    val amountText = formatAmount(voucher.totalAmount.amount, voucher.totalAmount.currency)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .then(
                    if (!loading) {
                        Modifier
                            .springPressScale(interaction, pressed = 0.985f)
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(),
                                role = Role.Button,
                                onClick = onClick
                            )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.bone(loading, widthFraction = 0.7f)
                )
                if (subtitle != null || loading) {
                    Text(
                        text = subtitle ?: "Cliente · 24/10 09:30",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.bone(loading, widthFraction = 0.55f)
                    )
                }
                if (stacked) {
                    VoucherTrailing(
                        amountText = amountText,
                        statusText = stringResource(statusLabel),
                        statusKind = statusKind,
                        loading = loading,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (!stacked) {
                VoucherTrailing(
                    amountText = amountText,
                    statusText = stringResource(statusLabel),
                    statusKind = statusKind,
                    loading = loading,
                    horizontalAlignment = Alignment.End
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun VoucherTrailing(
    amountText: String,
    statusText: String,
    statusKind: com.andeva.atelier.mobile.core.designsystem.component.ChipKind,
    loading: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.bone(loading)
        )
        if (loading) {
            Text(
                text = "Aceptado",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.bone(loading = true)
            )
        } else {
            AtelierChip(text = statusText, kind = statusKind)
        }
    }
}

@Composable
private fun VouchersSheets(state: VouchersUiState, callbacks: VouchersCallbacks) {
    when (val sheet = state.sheet) {
        is VoucherSheet.Detail -> {
            val voucher = sheet.voucher
            val code = formatVoucherCode(voucher.serie, voucher.number)
            val title = stringResource(R.string.cashier_voucher_name, stringResource(voucher.type.labelRes()), code)
            AtelierBottomSheet(
                title = title,
                onDismiss = callbacks.onDismissSheet,
                actions = {
                    if (!voucher.pdfUrl.isNullOrBlank()) {
                        AtelierAdaptiveRow(equalWidthInRow = true) {
                            AtelierButton(
                                text = stringResource(R.string.cashier_doc_open_pdf),
                                onClick = callbacks.onOpenPdf,
                                style = AtelierButtonStyle.Tonal,
                                enabled = state.isOnline && !state.isDownloadingDocument,
                                loading = state.isDownloadingDocument
                            )
                            AtelierButton(
                                text = stringResource(R.string.cashier_doc_share_pdf),
                                onClick = callbacks.onSharePdf,
                                style = AtelierButtonStyle.Outlined,
                                enabled = state.isOnline && !state.isDownloadingDocument
                            )
                        }
                    }
                    if (!voucher.xmlUrl.isNullOrBlank() || !voucher.cdrUrl.isNullOrBlank()) {
                        AtelierAdaptiveRow(equalWidthInRow = true) {
                            if (!voucher.xmlUrl.isNullOrBlank()) {
                                AtelierButton(
                                    text = stringResource(R.string.cashier_doc_share_xml),
                                    onClick = callbacks.onShareXml,
                                    style = AtelierButtonStyle.Outlined,
                                    enabled = state.isOnline && !state.isDownloadingDocument
                                )
                            }
                            if (!voucher.cdrUrl.isNullOrBlank()) {
                                AtelierButton(
                                    text = stringResource(R.string.cashier_doc_share_cdr),
                                    onClick = callbacks.onShareCdr,
                                    style = AtelierButtonStyle.Outlined,
                                    enabled = state.isOnline && !state.isDownloadingDocument
                                )
                            }
                        }
                    }
                    if (voucher.status == VoucherStatus.ISSUED) {
                        AtelierButton(
                            text = stringResource(R.string.cashier_sunat_refresh_status),
                            onClick = callbacks.onRefreshSunat,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Tonal,
                            enabled = state.isOnline && !state.isRefreshingSunat,
                            loading = state.isRefreshingSunat
                        )
                    }
                    if (state.canIssueSunat && voucher.canIssueActions) {
                        AtelierButton(
                            text = stringResource(R.string.cashier_nc_action),
                            onClick = callbacks.onStartCreditNote,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Tonal,
                            enabled = state.isOnline
                        )
                        AtelierButton(
                            text = stringResource(R.string.cashier_void_action),
                            onClick = callbacks.onStartVoid,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Destructive,
                            enabled = state.isOnline
                        )
                    }
                    if (!state.isOnline) AtelierRequiresConnection()
                    AtelierButton(
                        text = stringResource(R.string.cashier_vouchers_close),
                        onClick = callbacks.onDismissSheet,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text
                    )
                }
            ) {
                VoucherDetailSheetContent(voucher = voucher, canIssueSunat = state.canIssueSunat, error = state.sheetError?.asString())
            }
        }
        is VoucherSheet.Void -> {
            val voucherLabel = stringResource(
                R.string.cashier_voucher_name,
                stringResource(sheet.voucher.type.labelRes()),
                sheet.form.voucherLabel
            )
            AtelierBottomSheet(
                title = stringResource(R.string.cashier_void_title, voucherLabel),
                onDismiss = callbacks.onDismissSheet,
                actions = {
                    AtelierButton(
                        text = stringResource(R.string.cashier_void_confirm),
                        onClick = callbacks.onConfirmVoid,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.DestructiveFilled,
                        enabled = sheet.form.canSubmit && state.isOnline,
                        loading = state.isSubmitting
                    )
                    if (!state.isOnline) AtelierRequiresConnection()
                    AtelierButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = callbacks.onBackToDetail,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !state.isSubmitting
                    )
                }
            ) {
                VoidSheetContent(
                    form = sheet.form,
                    onReasonChange = callbacks.onVoidReason,
                    error = state.sheetError?.asString()
                )
            }
        }
        is VoucherSheet.CreditNote -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_nc_title),
            onDismiss = callbacks.onDismissSheet,
            actions = {
                AtelierButton(
                    text = stringResource(
                        R.string.cashier_nc_confirm,
                        formatAmount(sheet.form.parsedAmount ?: BigDecimal.ZERO, sheet.form.currency)
                    ),
                    onClick = callbacks.onConfirmCreditNote,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sheet.form.canSubmit && state.isOnline,
                    loading = state.isSubmitting
                )
                if (!state.isOnline) AtelierRequiresConnection()
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = callbacks.onBackToDetail,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isSubmitting
                )
            }
        ) {
            CreditNoteSheetContent(
                form = sheet.form,
                onReasonChange = callbacks.onCreditNoteReason,
                onAmountChange = callbacks.onCreditNoteAmount,
                error = state.sheetError?.asString(),
                onSerieChange = callbacks.onCreditNoteSerie
            )
        }
        null -> Unit
    }
}

@Composable
private fun VoucherDetailSheetContent(voucher: Voucher, canIssueSunat: Boolean, error: String? = null) {
    val (statusLabel, statusKind) = voucher.status.chip()
    val (paymentLabel, paymentKind) = voucher.paymentStatus.chip()
    val currency = voucher.totalAmount.currency

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtelierChip(text = stringResource(statusLabel), kind = statusKind)
        if (voucher.isActiveSale) {
            AtelierChip(text = stringResource(paymentLabel), kind = paymentKind)
        }
    }
    voucher.issuedAt?.let { issuedAt ->
        Text(
            text = stringResource(R.string.cashier_vouchers_issued_at, issuedAt.formatLimaDateTime()),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
    }
    if (!voucher.sunatResponseCode.isNullOrBlank() || !voucher.sunatDescription.isNullOrBlank()) {
        Text(
            text = stringResource(
                R.string.cashier_vouchers_cdr_code,
                voucher.sunatResponseCode.orEmpty().ifBlank { "—" },
                voucher.sunatDescription.orEmpty().ifBlank { stringResource(statusLabel) }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
    }
    voucher.customerInfo?.let { customer ->
        Text(
            text = stringResource(R.string.cashier_vouchers_customer_label),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = customer.legalName.ifBlank { "—" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        val docLine = listOfNotNull(
            customer.documentType?.let { stringResource(it.labelRes()) },
            customer.taxId.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        if (docLine.isNotBlank()) {
            Text(
                text = docLine,
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        customer.fiscalAddress?.takeIf { it.isNotBlank() }?.let { address ->
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        AmountRow(stringResource(R.string.cashier_subtotal), formatAmount(voucher.subtotal.amount, currency))
        AmountRow(stringResource(R.string.cashier_igv, 18), formatAmount(voucher.igvAmount.amount, currency))
        AmountRow(stringResource(R.string.cashier_total), formatAmount(voucher.totalAmount.amount, currency), emphasized = true)
        if (voucher.isActiveSale) {
            AmountRow(stringResource(R.string.cashier_collected), formatAmount(voucher.paidAmount.amount, currency))
            AmountRow(
                stringResource(R.string.cashier_balance),
                formatAmount(voucher.pendingBalance.amount, currency),
                emphasized = true
            )
        }
    }
    if (voucher.lines.isNotEmpty()) {
        Text(
            text = stringResource(R.string.cashier_vouchers_lines_label),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        voucher.lines.forEach { line ->
            AmountRow(
                label = stringResource(
                    R.string.cashier_issue_line_detail,
                    line.description,
                    line.quantity.stripTrailingZeros().toPlainString()
                ),
                value = formatAmount(line.totalLine ?: line.unitPrice ?: BigDecimal.ZERO, currency)
            )
        }
    }
    if (canIssueSunat && !voucher.canIssueActions) {
        Text(
            text = stringResource(R.string.cashier_vouchers_no_actions),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
    }
    error?.let {
        AtelierBanner(
            message = it,
            icon = Icons.Outlined.Info,
            kind = BannerKind.Bad
        )
    }
}

@StringRes
private fun VoucherFilter.labelRes(): Int = when (this) {
    VoucherFilter.ALL -> R.string.cashier_vouchers_filter_all
    VoucherFilter.ACCEPTED -> R.string.cashier_vouchers_filter_accepted
    VoucherFilter.WITH_BALANCE -> R.string.cashier_vouchers_filter_balance
    VoucherFilter.VOIDED -> R.string.cashier_vouchers_filter_voided
    VoucherFilter.CREDIT_NOTES -> R.string.cashier_vouchers_filter_credit_notes
}

// region Previews

private fun pen(amount: String) = Money(BigDecimal(amount), Money.PEN)

private val previewVouchers = listOf(
    Voucher(
        id = "v1",
        branchId = "b1",
        customerId = "c1",
        workOrderId = "wo1",
        type = VoucherType.FACTURA,
        serie = "F001",
        number = 128,
        subtotal = pen("1000.00"),
        igvAmount = pen("180.00"),
        totalAmount = pen("1180.00"),
        paidAmount = pen("500.00"),
        pendingBalance = pen("680.00"),
        paymentStatus = VoucherPaymentStatus.PARTIAL,
        status = VoucherStatus.ACCEPTED_SUNAT,
        customerInfo = CustomerFiscalInfo(FiscalDocumentType.RUC, "20512345678", "Transportes Lima S.A.C.", "Av. Colonial 420"),
        pdfUrl = null,
        xmlUrl = null,
        cdrUrl = null,
        sunatResponseCode = "0",
        sunatDescription = "La Factura F001-00000128 ha sido aceptada",
        lines = listOf(
            VoucherLine("l1", "s1", VoucherItemType.SERVICE, "Afinamiento mayor", BigDecimal.ONE, BigDecimal("1000.00"), BigDecimal("1180.00"), BigDecimal("180.00"), BigDecimal("1180.00"))
        ),
        payments = emptyList(),
        issuedAt = Instant.parse("2026-11-16T15:30:00Z"),
        currency = Money.PEN
    ),
    Voucher(
        id = "v2",
        branchId = "b1",
        customerId = "c2",
        workOrderId = "wo2",
        type = VoucherType.BOLETA,
        serie = "B001",
        number = 944,
        subtotal = pen("254.24"),
        igvAmount = pen("45.76"),
        totalAmount = pen("300.00"),
        paidAmount = pen("300.00"),
        pendingBalance = pen("0.00"),
        paymentStatus = VoucherPaymentStatus.PAID,
        status = VoucherStatus.ACCEPTED_SUNAT,
        customerInfo = CustomerFiscalInfo(FiscalDocumentType.DNI, "45871236", "Rosa Quispe Huamán", null),
        pdfUrl = null,
        xmlUrl = null,
        cdrUrl = null,
        sunatResponseCode = "0",
        sunatDescription = "Aceptado",
        lines = emptyList(),
        payments = emptyList(),
        issuedAt = Instant.parse("2026-11-16T17:10:00Z"),
        currency = Money.PEN
    ),
    Voucher(
        id = "v3",
        branchId = "b1",
        customerId = "c3",
        workOrderId = "wo3",
        type = VoucherType.NOTA_CREDITO,
        serie = "FC01",
        number = 12,
        subtotal = pen("100.00"),
        igvAmount = pen("18.00"),
        totalAmount = pen("118.00"),
        paidAmount = pen("0.00"),
        pendingBalance = pen("0.00"),
        paymentStatus = VoucherPaymentStatus.PENDING,
        status = VoucherStatus.ACCEPTED_SUNAT,
        customerInfo = CustomerFiscalInfo(FiscalDocumentType.RUC, "20512345678", "Transportes Lima S.A.C.", null),
        pdfUrl = null,
        xmlUrl = null,
        cdrUrl = null,
        sunatResponseCode = "0",
        sunatDescription = "Nota de crédito aceptada",
        lines = emptyList(),
        payments = emptyList(),
        issuedAt = Instant.parse("2026-11-16T18:20:00Z"),
        currency = Money.PEN
    ),
    Voucher(
        id = "v4",
        branchId = "b1",
        customerId = "c4",
        workOrderId = "wo4",
        type = VoucherType.BOLETA,
        serie = "B001",
        number = 940,
        subtotal = pen("169.49"),
        igvAmount = pen("30.51"),
        totalAmount = pen("200.00"),
        paidAmount = pen("0.00"),
        pendingBalance = pen("200.00"),
        paymentStatus = VoucherPaymentStatus.PENDING,
        status = VoucherStatus.VOIDED,
        customerInfo = CustomerFiscalInfo(FiscalDocumentType.DNI, "70123456", "Carlos Mendoza", null),
        pdfUrl = null,
        xmlUrl = null,
        cdrUrl = null,
        sunatResponseCode = null,
        sunatDescription = "Anulado por error en monto",
        lines = emptyList(),
        payments = emptyList(),
        issuedAt = Instant.parse("2026-11-15T14:00:00Z"),
        currency = Money.PEN
    )
)

private val previewState = VouchersUiState(
    vouchers = previewVouchers,
    hasLoaded = true,
    canRead = true,
    canIssueSunat = true
)

private val noVouchersCallbacks = VouchersCallbacks(
    onBack = {}, onRefresh = {}, onFilterChange = {}, onOpenVoucher = {}, onStartVoid = {},
    onVoidReason = {}, onConfirmVoid = {}, onStartCreditNote = {}, onCreditNoteReason = {},
    onCreditNoteAmount = {}, onConfirmCreditNote = {}, onBackToDetail = {}, onDismissSheet = {}
)

@Composable
private fun PreviewContent(state: VouchersUiState) {
    Surface(color = MaterialTheme.colorScheme.background) {
        VouchersContent(state = state, callbacks = noVouchersCallbacks)
    }
}

@Preview(name = "Comprobantes · claro", widthDp = 390, heightDp = 844)
@Composable
private fun VouchersPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Comprobantes · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VouchersPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Comprobantes · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun VouchersPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(vouchers = emptyList(), hasLoaded = false))
}

@Preview(name = "Comprobantes · detalle", widthDp = 390, showBackground = true)
@Composable
private fun VouchersDetailPreview() = AtelierWorkshopTheme(darkTheme = false) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VoucherDetailSheetContent(voucher = previewVouchers.first(), canIssueSunat = true)
        }
    }
}

// endregion
