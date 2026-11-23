package com.andeva.atelier.mobile.features.cashier.presentation.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.cashier.application.DeliverVehicleUseCase
import com.andeva.atelier.mobile.features.cashier.application.DownloadExitPassUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetVoucherPaymentsUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetWorkOrderVouchersUseCase
import com.andeva.atelier.mobile.features.cashier.application.IssueVoucherUseCase
import com.andeva.atelier.mobile.features.cashier.application.MarkWorkOrderPaidUseCase
import com.andeva.atelier.mobile.features.cashier.application.ObserveWorkOrderVouchersUseCase
import com.andeva.atelier.mobile.features.cashier.application.PrepareVoucherUseCase
import com.andeva.atelier.mobile.features.cashier.application.RegisterPaymentUseCase
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.PaymentOutcomeUnknownException
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.toCashierText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObserveWorkOrderUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class WorkOrderCheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSession: ObserveSessionUseCase,
    observeOrder: ObserveWorkOrderUseCase,
    private val getWorkOrderDetail: GetWorkOrderDetailUseCase,
    observeVouchers: ObserveWorkOrderVouchersUseCase,
    private val getVouchers: GetWorkOrderVouchersUseCase,
    private val getPayments: GetVoucherPaymentsUseCase,
    private val prepareVoucher: PrepareVoucherUseCase,
    private val issueVoucher: IssueVoucherUseCase,
    private val registerPayment: RegisterPaymentUseCase,
    private val markPaid: MarkWorkOrderPaidUseCase,
    private val downloadExitPass: DownloadExitPassUseCase,
    private val deliverVehicle: DeliverVehicleUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(WorkOrderCheckoutUiState(workOrderId = workOrderId))
    val uiState: StateFlow<WorkOrderCheckoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canReadInvoices = session?.has(Permission.INVOICES_READ) == true,
                        canIssueVoucher = session?.has(Permission.INVOICES_ISSUE_SUNAT) == true,
                        canRegisterPayment = session?.has(Permission.PAYMENTS_CREATE) == true,
                        canIssueExitPass = session?.has(Permission.EXIT_PASSES_ISSUE) == true,
                        canDeliver = session?.has(Permission.VEHICLE_HANDOVER_EXECUTE) == true
                    )
                }
            }
        }
        // Una orden pagada sale de la caché de la sede: solo se toma de Room cuando hay fila.
        viewModelScope.launch { observeOrder(workOrderId).collect { order -> if (order != null) mergeOrder(order) } }
        viewModelScope.launch {
            observeVouchers(workOrderId).collect { cached -> _uiState.update { it.copy(cachedVouchers = cached) } }
        }
        viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } } }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val canReadInvoices = observeSession().firstOrNull()?.has(Permission.INVOICES_READ) == true
        var failure: Throwable? = null
        getWorkOrderDetail(workOrderId)
            .onSuccess { detail -> mergeOrder(detail.workOrder) }
            .onFailure { failure = it }
        if (canReadInvoices) {
            getVouchers(workOrderId)
                .onSuccess { vouchers ->
                    _uiState.update { it.copy(liveVouchers = vouchers) }
                    _uiState.value.activeVoucher?.let { loadPayments(it.id) }
                }
                .onFailure { failure = failure ?: it }
        }
        _uiState.update { it.copy(isRefreshing = false, hasLoaded = true, error = failure?.toCashierText()) }
    }

    private suspend fun loadPayments(voucherId: String) {
        getPayments(voucherId).onSuccess { payments -> _uiState.update { it.copy(payments = payments) } }
    }

    private fun mergeOrder(new: WorkOrder) = _uiState.update { state ->
        state.copy(
            order = new.copy(
                plate = new.plate ?: state.order?.plate,
                customerName = new.customerName ?: state.order?.customerName
            )
        )
    }

    /** Las acciones son en línea y de una en una (docs/06-offline-sync.md). */
    private fun canAct(): Boolean = _uiState.value.let { it.isOnline && it.busy == null }

    // region Comprobante

    fun onPrepareVoucher() {
        if (!_uiState.value.canIssueVoucher || !canAct()) return
        _uiState.update { it.copy(busy = CheckoutAction.Preparing, actionError = null, notice = null) }
        viewModelScope.launch {
            prepareVoucher(workOrderId)
                .onSuccess { draft -> _uiState.update { it.copy(sheet = CheckoutSheet.Issue(draft.toForm()), sheetError = null) } }
                .onFailure { e -> _uiState.update { it.copy(actionError = e.toCashierText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    fun onIssueType(type: VoucherType) = updateIssue { it.withType(type) }
    fun onIssueDocumentType(type: FiscalDocumentType) = updateIssue { it.copy(documentType = type) }
    fun onIssueTaxId(value: String) = updateIssue { it.copy(taxId = value.filter { c -> c.isLetterOrDigit() }.take(20)) }
    fun onIssueName(value: String) = updateIssue { it.copy(legalName = value) }
    fun onIssueAddress(value: String) = updateIssue { it.copy(address = value) }

    fun onIssueLineName(index: Int, value: String) = updateIssue { form ->
        form.copy(lines = form.lines.mapIndexed { i, line -> if (i == index) line.copy(description = value) else line })
    }

    private fun updateIssue(change: (IssueForm) -> IssueForm) = _uiState.update { state ->
        val sheet = state.sheet as? CheckoutSheet.Issue ?: return@update state
        state.copy(sheet = CheckoutSheet.Issue(change(sheet.form)), sheetError = null)
    }

    fun onConfirmIssue() {
        val form = (_uiState.value.sheet as? CheckoutSheet.Issue)?.form ?: return
        if (!form.canSubmit || !canAct()) return
        _uiState.update { it.copy(busy = CheckoutAction.Issuing, sheetError = null) }
        viewModelScope.launch {
            issueVoucher(form.toDraft())
                .onSuccess { outcome ->
                    val notice = when (outcome) {
                        is IssueOutcome.Issued -> R.string.cashier_issue_done
                        is IssueOutcome.AlreadyIssued -> R.string.cashier_issue_already
                        is IssueOutcome.IssuedPendingDetails -> R.string.cashier_issue_details_pending
                    }
                    _uiState.update { it.copy(sheet = null, notice = UiText.Resource(notice)) }
                    load()
                }
                .onFailure { e -> _uiState.update { it.copy(sheetError = e.toCashierText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    private fun VoucherDraft.toForm() = IssueForm(
        base = this,
        type = type,
        documentType = if (type == VoucherType.FACTURA) FiscalDocumentType.RUC
        else customerInfo.documentType ?: FiscalDocumentType.DNI,
        taxId = customerInfo.taxId,
        legalName = customerInfo.legalName,
        address = customerInfo.fiscalAddress.orEmpty(),
        lines = lines
    )

    // endregion

    // region Cobros

    fun onOpenPayment() {
        val voucher = _uiState.value.activeVoucher ?: return
        if (!_uiState.value.canRegisterPayment || !canAct() || voucher.pending.signum() <= 0) return
        val amount = voucher.pending.setScale(2, RoundingMode.HALF_UP).toPlainString()
        _uiState.update {
            it.copy(sheet = CheckoutSheet.Payment(PaymentForm(voucher, amount)), sheetError = null, notice = null, actionError = null)
        }
    }

    fun onPaymentAmount(value: String) = updatePayment { it.copy(amount = value) }
    fun onPaymentMethod(method: PaymentMethod) = updatePayment { it.copy(method = method) }
    fun onPaymentReference(value: String) = updatePayment { it.copy(reference = value.take(PaymentForm.MAX_REFERENCE)) }

    private fun updatePayment(change: (PaymentForm) -> PaymentForm) = _uiState.update { state ->
        val sheet = state.sheet as? CheckoutSheet.Payment ?: return@update state
        state.copy(sheet = CheckoutSheet.Payment(change(sheet.form)), sheetError = null)
    }

    fun onConfirmPayment() {
        val form = (_uiState.value.sheet as? CheckoutSheet.Payment)?.form ?: return
        val amount = form.parsedAmount ?: return
        if (!canAct()) return
        _uiState.update { it.copy(busy = CheckoutAction.Paying, sheetError = null) }
        viewModelScope.launch {
            registerPayment(form.voucher.id, amount, form.method, form.reference.trim().ifBlank { null })
                .onSuccess { _ ->
                    _uiState.update { it.copy(sheet = null, notice = UiText.Resource(R.string.cashier_payment_done)) }
                    load()
                }
                .onFailure { e ->
                    if (e is PaymentOutcomeUnknownException) {
                        // No se sabe si cobró: se cierra la hoja para que no se reintente a ciegas y se relee el historial.
                        _uiState.update { it.copy(sheet = null, actionError = e.toCashierText()) }
                        load()
                    } else {
                        _uiState.update { it.copy(sheetError = e.toCashierText()) }
                    }
                }
            _uiState.update { it.copy(busy = null) }
        }
    }

    fun onMarkPaid() {
        if (!_uiState.value.canMarkPaid || !canAct()) return
        _uiState.update { it.copy(busy = CheckoutAction.MarkingPaid, actionError = null, notice = null) }
        viewModelScope.launch {
            markPaid(workOrderId)
                .onSuccess { order ->
                    mergeOrder(order)
                    _uiState.update { it.copy(notice = UiText.Resource(R.string.cashier_mark_paid_done)) }
                }
                .onFailure { e -> _uiState.update { it.copy(actionError = e.toCashierText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    // endregion

    // region Pase de salida y entrega

    fun onDownloadExitPass() {
        val state = _uiState.value
        if (!state.canIssueExitPass || !state.isOrderPaid || !canAct()) return
        _uiState.update { it.copy(busy = CheckoutAction.ExitPass, actionError = null, notice = null) }
        viewModelScope.launch {
            downloadExitPass(workOrderId)
                .onSuccess { file -> _uiState.update { state -> state.copy(exitPass = file) } }
                .onFailure { e -> _uiState.update { it.copy(actionError = e.toCashierText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    fun onExitPassHandled() = _uiState.update { it.copy(exitPass = null) }

    fun onExitPassOpenFailed() =
        _uiState.update { it.copy(exitPass = null, actionError = UiText.Resource(R.string.cashier_exit_pass_no_app)) }

    fun onOpenDeliver() {
        val state = _uiState.value
        if (!state.canDeliver || !state.isOrderPaid || state.delivered || !canAct()) return
        _uiState.update { it.copy(sheet = CheckoutSheet.Deliver, sheetError = null, notice = null, actionError = null) }
    }

    fun onConfirmDeliver() {
        if (_uiState.value.sheet != CheckoutSheet.Deliver || !canAct()) return
        _uiState.update { it.copy(busy = CheckoutAction.Delivering, sheetError = null) }
        viewModelScope.launch {
            deliverVehicle(workOrderId)
                .onSuccess { order ->
                    mergeOrder(order)
                    _uiState.update {
                        it.copy(sheet = null, delivered = true, notice = UiText.Resource(R.string.cashier_deliver_done))
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(sheetError = e.toCashierText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    // endregion

    fun onDismissSheet() {
        if (_uiState.value.busy != null) return
        _uiState.update { it.copy(sheet = null, sheetError = null) }
    }
}
