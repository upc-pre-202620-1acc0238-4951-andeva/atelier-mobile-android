package com.andeva.atelier.mobile.features.cashier.presentation.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.AmountRow
import com.andeva.atelier.mobile.features.cashier.presentation.ChoiceGrid
import com.andeva.atelier.mobile.features.cashier.presentation.CreditNoteSheetContent
import com.andeva.atelier.mobile.features.cashier.presentation.VoidSheetContent
import com.andeva.atelier.mobile.features.cashier.presentation.formatAmount
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import java.math.BigDecimal

@Composable
internal fun CheckoutSheets(state: WorkOrderCheckoutUiState, callbacks: CheckoutCallbacks) {
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
        is CheckoutSheet.Void -> {
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
                        loading = state.busy == CheckoutAction.Voiding
                    )
                    if (!state.isOnline) AtelierRequiresConnection()
                    AtelierButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = callbacks.onDismissSheet,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = state.busy == null
                    )
                }
            ) {
                OrderIdentity(state.order)
                VoidSheetContent(
                    form = sheet.form,
                    onReasonChange = callbacks.onVoidReason,
                    error = state.sheetError?.asString()
                )
            }
        }
        is CheckoutSheet.CreditNote -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_nc_title),
            onDismiss = callbacks.onDismissSheet,
            actions = {
                SheetActions(
                    confirm = stringResource(
                        R.string.cashier_nc_confirm,
                        formatAmount(sheet.form.parsedAmount ?: BigDecimal.ZERO, sheet.form.currency)
                    ),
                    enabled = sheet.form.canSubmit && state.isOnline,
                    loading = state.busy == CheckoutAction.IssuingCreditNote,
                    online = state.isOnline,
                    onConfirm = callbacks.onConfirmCreditNote,
                    onCancel = callbacks.onDismissSheet
                )
            }
        ) {
            OrderIdentity(state.order)
            CreditNoteSheetContent(
                form = sheet.form,
                onReasonChange = callbacks.onCreditNoteReason,
                onAmountChange = callbacks.onCreditNoteAmount,
                error = state.sheetError?.asString()
            )
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
internal fun IssueSheetContent(
    order: WorkOrder?,
    form: IssueForm,
    error: String?,
    callbacks: CheckoutCallbacks
) {
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
internal fun PaymentSheetContent(
    order: WorkOrder?,
    form: PaymentForm,
    error: String?,
    callbacks: CheckoutCallbacks
) {
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
    if (form.requiresReference) {
        AtelierTextField(
            label = stringResource(R.string.cashier_payment_reference),
            value = form.reference,
            onValueChange = callbacks.onPaymentReference,
            placeholder = stringResource(R.string.cashier_payment_reference_hint)
        )
        if (form.reference.isBlank()) {
            Text(
                text = stringResource(R.string.cashier_payment_reference_required),
                style = MaterialTheme.typography.labelSmall,
                color = AtelierTheme.colors.inkSecondary
            )
        }
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
