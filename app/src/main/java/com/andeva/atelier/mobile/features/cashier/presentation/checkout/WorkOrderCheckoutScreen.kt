package com.andeva.atelier.mobile.features.cashier.presentation.checkout

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
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
import com.andeva.atelier.mobile.features.cashier.presentation.ChoiceGrid
import com.andeva.atelier.mobile.features.cashier.presentation.chip
import com.andeva.atelier.mobile.features.cashier.presentation.formatAmount
import com.andeva.atelier.mobile.features.cashier.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes
import com.andeva.atelier.mobile.features.cashier.presentation.shareOrOpenPdf
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.math.BigDecimal
import java.time.Instant

/** Acciones de la pantalla, para que [WorkOrderCheckoutContent] no conozca al ViewModel. */
private class CheckoutCallbacks(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onPrepareVoucher: () -> Unit,
    val onOpenPayment: () -> Unit,
    val onMarkPaid: () -> Unit,
    val onDownloadExitPass: () -> Unit,
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
    val onConfirmDeliver: () -> Unit
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
    val chooserTitle = stringResource(R.string.cashier_exit_pass_chooser)
    LaunchedEffect(state.exitPass) {
        val file = state.exitPass ?: return@LaunchedEffect
        if (shareOrOpenPdf(context, file, chooserTitle)) viewModel.onExitPassHandled() else viewModel.onExitPassOpenFailed()
    }
    WorkOrderCheckoutContent(
        state = state,
        callbacks = CheckoutCallbacks(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onPrepareVoucher = viewModel::onPrepareVoucher,
            onOpenPayment = viewModel::onOpenPayment,
            onMarkPaid = viewModel::onMarkPaid,
            onDownloadExitPass = viewModel::onDownloadExitPass,
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
            onConfirmDeliver = viewModel::onConfirmDeliver
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkOrderCheckoutContent(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
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
                    messages(state)
                    voucherStep(state, callbacks)
                    paymentsStep(state, callbacks)
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

private fun LazyListScope.messages(state: WorkOrderCheckoutUiState) {
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
                AtelierOfflineState(modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                AtelierErrorState(
                    message = state.error.asString(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// region Pasos

@Composable
private fun StepHeader(number: Int, @androidx.annotation.StringRes label: Int, chip: (@Composable () -> Unit)? = null) {
    AtelierSectionHeader(
        title = stringResource(R.string.cashier_step_title, number, stringResource(label)),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        action = chip
    )
}

@Composable
private fun StepHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = AtelierTheme.colors.inkSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AtelierButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            enabled = enabled && state.isOnline && (state.busy == null || state.busy == action),
            loading = state.busy == action
        )
        if (!state.isOnline) AtelierRequiresConnection()
    }
}

private fun LazyListScope.voucherStep(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.canReadInvoices && !state.canIssueVoucher) return
    item(key = "step-voucher") {
        StepHeader(1, R.string.cashier_step_voucher) {
            state.activeVoucher?.let { voucher ->
                val (label, kind) = voucher.status.chip()
                AtelierChip(stringResource(label), kind = kind)
            }
        }
    }
    when {
        state.showSkeleton -> items(2, key = { "voucher-bone-$it" }) {
            AtelierListRow(title = "Boleta B001-00000001", subtitle = "Estado", loading = true)
        }
        else -> {
            items(state.vouchers, key = { "voucher-${it.id}" }) { voucher -> VoucherBlock(voucher) }
            if (state.activeVoucher == null) {
                item(key = "voucher-prepare") {
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
                            text = stringResource(R.string.cashier_voucher_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoucherBlock(voucher: VoucherView) {
    val name = stringResource(
        R.string.cashier_voucher_name,
        stringResource(voucher.type.labelRes()),
        "${voucher.serie}-${voucher.number.toString().padStart(8, '0')}"
    )
    val (statusLabel, statusKind) = voucher.status.chip()
    Column(modifier = Modifier.fillMaxWidth()) {
        AtelierListRow(
            title = name,
            showDivider = false,
            trailing = { AtelierChip(stringResource(statusLabel), kind = statusKind) }
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Subtotal e IGV tal como los da el backend; solo con la cabecera en caché va el total.
            if (voucher.subtotal != null && voucher.igv != null) {
                AmountRow(stringResource(R.string.cashier_subtotal), formatAmount(voucher.subtotal, voucher.currency))
                AmountRow(stringResource(R.string.cashier_igv, 18), formatAmount(voucher.igv, voucher.currency))
            }
            AmountRow(stringResource(R.string.cashier_total), formatAmount(voucher.total, voucher.currency), emphasized = true)
        }
    }
}

private fun LazyListScope.paymentsStep(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.canReadInvoices && !state.canRegisterPayment) return
    val voucher = state.activeVoucher
    item(key = "step-payments") {
        StepHeader(2, R.string.cashier_step_payments) {
            voucher?.let {
                val (label, kind) = it.paymentStatus.chip()
                AtelierChip(stringResource(label), kind = kind)
            }
        }
    }
    if (voucher == null) {
        if (!state.showSkeleton) item(key = "payments-needs-voucher") { StepHint(stringResource(R.string.cashier_payments_needs_voucher)) }
        return
    }
    item(key = "payments-amounts") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            AmountRow(stringResource(R.string.cashier_collected), formatAmount(voucher.paid, voucher.currency))
            AmountRow(stringResource(R.string.cashier_balance), formatAmount(voucher.pending, voucher.currency), emphasized = true)
        }
    }
    if (state.payments.isEmpty()) {
        item(key = "payments-empty") { StepHint(stringResource(R.string.cashier_payments_empty)) }
    } else {
        items(state.payments, key = { "payment-${it.id}" }) { payment -> PaymentRow(payment, voucher.currency) }
    }
    if (state.canRegisterPayment && voucher.pending.signum() > 0) {
        item(key = "payments-register") {
            StepAction(
                text = stringResource(R.string.cashier_payment_register),
                onClick = callbacks.onOpenPayment,
                state = state,
                action = CheckoutAction.Paying
            )
        }
    }
    if (state.canMarkPaid) {
        item(key = "payments-mark-paid") {
            StepAction(
                text = stringResource(R.string.cashier_mark_paid),
                onClick = callbacks.onMarkPaid,
                state = state,
                action = CheckoutAction.MarkingPaid
            )
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, currency: String?) {
    val time = payment.paidAt?.formatLimaDateTime()
    val reference = payment.transactionReference?.takeIf { it.isNotBlank() }
    AtelierListRow(
        title = stringResource(payment.method.labelRes()),
        subtitle = listOfNotNull(time, reference).joinToString(" · ").ifBlank { null },
        trailing = {
            Text(
                text = formatAmount(payment.amount.amount, payment.amount.currency.ifBlank { currency }),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    )
}

private fun LazyListScope.exitPassStep(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.canIssueExitPass) return
    item(key = "step-exit-pass") {
        StepHeader(3, R.string.cashier_step_exit_pass)
        if (!state.isOrderPaid) StepHint(stringResource(R.string.cashier_exit_pass_needs_paid))
        StepAction(
            text = stringResource(R.string.cashier_exit_pass_download),
            onClick = callbacks.onDownloadExitPass,
            state = state,
            action = CheckoutAction.ExitPass,
            style = AtelierButtonStyle.Tonal,
            enabled = state.isOrderPaid
        )
    }
}

private fun LazyListScope.deliveryStep(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    if (!state.canDeliver) return
    item(key = "step-delivery") {
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

// endregion

// region Hojas de confirmación

@Composable
private fun CheckoutSheets(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
    when (val sheet = state.sheet) {
        is CheckoutSheet.Issue -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_issue_title),
            onDismiss = callbacks.onDismissSheet,
            actions = {
                SheetActions(
                    confirm = stringResource(
                        R.string.cashier_issue_confirm,
                        stringResource(sheet.form.type.labelRes()),
                        formatAmount(sheet.form.total, sheet.form.base.currency)
                    ),
                    enabled = sheet.form.canSubmit && state.isOnline,
                    loading = state.busy == CheckoutAction.Issuing,
                    online = state.isOnline,
                    onConfirm = callbacks.onConfirmIssue,
                    onCancel = callbacks.onDismissSheet
                )
            }
        ) {
            IssueSheetContent(state.order, sheet.form, state.sheetError?.asString(), callbacks)
        }
        is CheckoutSheet.Payment -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_payment_title),
            onDismiss = callbacks.onDismissSheet,
            actions = {
                SheetActions(
                    confirm = stringResource(
                        R.string.cashier_payment_confirm,
                        formatAmount(sheet.form.parsedAmount ?: BigDecimal.ZERO, sheet.form.voucher.currency)
                    ),
                    enabled = sheet.form.canSubmit && state.isOnline,
                    loading = state.busy == CheckoutAction.Paying,
                    online = state.isOnline,
                    onConfirm = callbacks.onConfirmPayment,
                    onCancel = callbacks.onDismissSheet
                )
            }
        ) {
            PaymentSheetContent(state.order, sheet.form, state.sheetError?.asString(), callbacks)
        }
        CheckoutSheet.Deliver -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_deliver_title),
            onDismiss = callbacks.onDismissSheet,
            actions = {
                SheetActions(
                    confirm = stringResource(R.string.cashier_deliver_confirm),
                    enabled = state.isOnline,
                    loading = state.busy == CheckoutAction.Delivering,
                    online = state.isOnline,
                    onConfirm = callbacks.onConfirmDeliver,
                    onCancel = callbacks.onDismissSheet
                )
            }
        ) {
            OrderIdentity(state.order)
            Text(
                text = stringResource(R.string.cashier_deliver_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            state.sheetError?.let { SheetError(it.asString()) }
        }
        null -> Unit
    }
}

@Composable
private fun SheetActions(
    confirm: String,
    enabled: Boolean,
    loading: Boolean,
    online: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AtelierButton(
        text = confirm,
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        loading = loading
    )
    if (!online) AtelierRequiresConnection()
    AtelierButton(
        text = stringResource(R.string.common_cancel),
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Text,
        enabled = !loading
    )
}

@Composable
private fun SheetError(message: String) {
    AtelierBanner(message = message, icon = Icons.Outlined.ErrorOutline)
}

/** La placa a la vista en cada confirmación: se cobra y se entrega este vehículo. */
@Composable
private fun OrderIdentity(order: WorkOrder?) {
    if (order == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        order.plate?.let { AtelierPlate(it, size = PlateSize.L) }
        order.customerName?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun IssueSheetContent(order: WorkOrder?, form: IssueForm, error: String?, callbacks: CheckoutCallbacks) {
    OrderIdentity(order)
    SheetLabel(stringResource(R.string.cashier_issue_type))
    ChoiceGrid(
        options = listOf(VoucherType.BOLETA, VoucherType.FACTURA),
        selected = form.type,
        label = { stringResource(it.labelRes()) },
        onSelect = callbacks.onIssueType,
        columns = 2
    )
    if (!form.isCompany) {
        SheetLabel(stringResource(R.string.cashier_issue_document))
        ChoiceGrid(
            options = listOf(FiscalDocumentType.DNI, FiscalDocumentType.CE, FiscalDocumentType.PASAPORTE),
            selected = form.documentType,
            label = { stringResource(it.labelRes()) },
            onSelect = callbacks.onIssueDocumentType
        )
    }
    AtelierTextField(
        label = stringResource(R.string.cashier_issue_document_number),
        value = form.taxId,
        onValueChange = callbacks.onIssueTaxId,
        isError = form.taxId.isNotEmpty() && !form.taxIdValid,
        errorText = stringResource(R.string.cashier_issue_document_error).takeIf { form.taxId.isNotEmpty() && !form.taxIdValid },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (form.documentType == FiscalDocumentType.PASAPORTE || form.documentType == FiscalDocumentType.CE) {
                KeyboardType.Ascii
            } else {
                KeyboardType.Number
            }
        )
    )
    AtelierTextField(
        label = stringResource(if (form.isCompany) R.string.cashier_issue_name_company else R.string.cashier_issue_name_person),
        value = form.legalName,
        onValueChange = callbacks.onIssueName,
        isError = !form.nameValid,
        errorText = stringResource(R.string.cashier_issue_name_error).takeIf { !form.nameValid }
    )
    if (form.isCompany) {
        AtelierTextField(
            label = stringResource(R.string.cashier_issue_address),
            value = form.address,
            onValueChange = callbacks.onIssueAddress
        )
    }
    SheetLabel(stringResource(R.string.cashier_issue_lines))
    form.lines.forEachIndexed { index, line ->
        if (line.needsDescription) {
            AtelierTextField(
                label = stringResource(R.string.cashier_issue_line_name),
                value = line.description,
                onValueChange = { callbacks.onIssueLineName(index, it) },
                placeholder = stringResource(R.string.cashier_issue_line_needs_name)
            )
        }
        AmountRow(
            label = if (line.needsDescription) {
                stringResource(R.string.cashier_issue_line_detail, line.quantity.stripTrailingZeros().toPlainString(), "")
                    .trim()
            } else {
                stringResource(
                    R.string.cashier_issue_line_detail,
                    line.description,
                    line.quantity.stripTrailingZeros().toPlainString()
                )
            },
            value = formatAmount(line.quantity * line.unitPriceWithIgv, form.base.currency)
        )
    }
    AmountRow(stringResource(R.string.cashier_subtotal), formatAmount(form.subtotal, form.base.currency))
    AmountRow(stringResource(R.string.cashier_igv, 18), formatAmount(form.igv, form.base.currency))
    AmountRow(stringResource(R.string.cashier_total), formatAmount(form.total, form.base.currency), emphasized = true)
    Text(
        text = stringResource(R.string.cashier_issue_irreversible),
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary
    )
    error?.let { SheetError(it) }
}

@Composable
private fun PaymentSheetContent(order: WorkOrder?, form: PaymentForm, error: String?, callbacks: CheckoutCallbacks) {
    val voucher = form.voucher
    OrderIdentity(order)
    Column {
        if (voucher.subtotal != null && voucher.igv != null) {
            AmountRow(stringResource(R.string.cashier_subtotal), formatAmount(voucher.subtotal, voucher.currency))
            AmountRow(stringResource(R.string.cashier_igv, 18), formatAmount(voucher.igv, voucher.currency))
        }
        AmountRow(stringResource(R.string.cashier_total), formatAmount(voucher.total, voucher.currency), emphasized = true)
        AmountRow(stringResource(R.string.cashier_collected), formatAmount(voucher.paid, voucher.currency))
        AmountRow(stringResource(R.string.cashier_balance), formatAmount(voucher.pending, voucher.currency), emphasized = true)
    }
    val amountInvalid = form.amount.isNotBlank() && form.parsedAmount == null
    AtelierTextField(
        label = stringResource(R.string.cashier_payment_amount),
        value = form.amount,
        onValueChange = callbacks.onPaymentAmount,
        isError = amountInvalid,
        errorText = stringResource(R.string.cashier_payment_amount_error).takeIf { amountInvalid },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
    SheetLabel(stringResource(R.string.cashier_payment_method))
    ChoiceGrid(
        options = PaymentMethod.entries,
        selected = form.method,
        label = { stringResource(it.labelRes()) },
        onSelect = callbacks.onPaymentMethod,
        columns = 2
    )
    if (form.method != PaymentMethod.CASH) {
        AtelierTextField(
            label = stringResource(R.string.cashier_payment_reference),
            value = form.reference,
            onValueChange = callbacks.onPaymentReference
        )
    }
    Text(
        text = stringResource(R.string.cashier_payment_irreversible),
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary
    )
    error?.let { SheetError(it) }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = AtelierTheme.colors.inkSecondary)
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
    onBack = {}, onRefresh = {}, onPrepareVoucher = {}, onOpenPayment = {}, onMarkPaid = {}, onDownloadExitPass = {},
    onOpenDeliver = {}, onDismissSheet = {}, onIssueType = {}, onIssueDocumentType = {}, onIssueTaxId = {},
    onIssueName = {}, onIssueAddress = {}, onIssueLineName = { _, _ -> }, onConfirmIssue = {}, onPaymentAmount = {},
    onPaymentMethod = {}, onPaymentReference = {}, onConfirmPayment = {}, onConfirmDeliver = {}
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
