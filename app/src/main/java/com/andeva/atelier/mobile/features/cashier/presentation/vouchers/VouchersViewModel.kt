package com.andeva.atelier.mobile.features.cashier.presentation.vouchers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.cashier.application.DownloadVoucherDocumentUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetBranchSeriesUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetVouchersUseCase
import com.andeva.atelier.mobile.features.cashier.application.IssueCreditNoteUseCase
import com.andeva.atelier.mobile.features.cashier.application.RefreshVoucherStatusUseCase
import com.andeva.atelier.mobile.features.cashier.application.VoidVoucherUseCase
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.CreditNoteForm
import com.andeva.atelier.mobile.features.cashier.presentation.VoidForm
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.CheckoutDocumentActionMode
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.PendingDocumentFile
import com.andeva.atelier.mobile.features.cashier.presentation.formatVoucherCode
import com.andeva.atelier.mobile.features.cashier.presentation.toCashierText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
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
class VouchersViewModel @Inject constructor(
    private val getVouchers: GetVouchersUseCase,
    private val voidVoucher: VoidVoucherUseCase,
    private val issueCreditNote: IssueCreditNoteUseCase,
    private val observeSession: ObserveSessionUseCase,
    connectivity: ConnectivityObserver,
    private val downloadVoucherDocument: DownloadVoucherDocumentUseCase? = null,
    private val refreshVoucherStatus: RefreshVoucherStatusUseCase? = null,
    private val getBranchSeries: GetBranchSeriesUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VouchersUiState())
    val uiState: StateFlow<VouchersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canRead = session?.has(Permission.INVOICES_READ) == true,
                        canIssueSunat = session?.has(Permission.INVOICES_ISSUE_SUNAT) == true
                    )
                }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && !_uiState.value.isOnline && _uiState.value.vouchers.isEmpty()
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val canRead = observeSession().firstOrNull()?.has(Permission.INVOICES_READ) == true
        if (!canRead) {
            _uiState.update { it.copy(hasLoaded = true, isRefreshing = false) }
            return
        }
        getVouchers()
            .onSuccess { list ->
                val sorted = list.sortedByDescending { it.issuedAt }
                _uiState.update { it.copy(vouchers = sorted, hasLoaded = true, isRefreshing = false, error = null) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(hasLoaded = true, isRefreshing = false, error = e.toCashierText()) }
            }
    }

    fun onFilterChange(filter: VoucherFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onOpenVoucher(voucher: Voucher) {
        _uiState.update { it.copy(sheet = VoucherSheet.Detail(voucher), sheetError = null, notice = null) }
    }

    fun onStartVoid() {
        val state = _uiState.value
        val voucher = (state.sheet as? VoucherSheet.Detail)?.voucher ?: return
        if (!state.canIssueSunat || !voucher.canIssueActions) return
        val label = formatVoucherCode(voucher.serie, voucher.number)
        _uiState.update {
            it.copy(sheet = VoucherSheet.Void(voucher, VoidForm(voucher.id, label)), sheetError = null)
        }
    }

    fun onVoidReason(value: String) = _uiState.update { state ->
        val sheet = state.sheet as? VoucherSheet.Void ?: return@update state
        state.copy(
            sheet = sheet.copy(form = sheet.form.copy(reason = value.take(CashierValidation.MAX_REASON_LENGTH))),
            sheetError = null
        )
    }

    fun onConfirmVoid() {
        val state = _uiState.value
        val sheet = state.sheet as? VoucherSheet.Void ?: return
        if (!state.canIssueSunat || !state.isOnline || state.isSubmitting || !sheet.form.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, sheetError = null) }
        viewModelScope.launch {
            voidVoucher(sheet.voucher.id, sheet.form.reason)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            sheet = null,
                            isSubmitting = false,
                            notice = UiText.Resource(R.string.cashier_void_done)
                        )
                    }
                    load()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, sheetError = e.toCashierText()) }
                }
        }
    }

    fun onStartCreditNote() {
        val state = _uiState.value
        val voucher = (state.sheet as? VoucherSheet.Detail)?.voucher ?: return
        if (!state.canIssueSunat || !voucher.canIssueActions) return
        val label = formatVoucherCode(voucher.serie, voucher.number)
        val initialAmount = voucher.totalAmount.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        val form = CreditNoteForm(
            referenceVoucherId = voucher.id,
            referenceLabel = label,
            maxAmount = voucher.totalAmount.amount,
            currency = voucher.totalAmount.currency,
            amount = initialAmount
        )
        _uiState.update { it.copy(sheet = VoucherSheet.CreditNote(voucher, form), sheetError = null) }
        viewModelScope.launch {
            val series = getBranchSeries?.invoke()?.getOrNull().orEmpty()
                .filter { it.active && it.type == VoucherType.NOTA_CREDITO && CashierValidation.isCreditNoteSeries(it.serie, voucher.type) }
            if (series.isNotEmpty()) {
                _uiState.update { current ->
                    val currentSheet = current.sheet as? VoucherSheet.CreditNote ?: return@update current
                    current.copy(
                        sheet = currentSheet.copy(
                            form = currentSheet.form.copy(
                                availableSeries = series,
                                selectedSerie = series.first().serie
                            )
                        )
                    )
                }
            }
        }
    }

    fun onCreditNoteSerie(value: String) = _uiState.update { state ->
        val sheet = state.sheet as? VoucherSheet.CreditNote ?: return@update state
        state.copy(sheet = sheet.copy(form = sheet.form.copy(selectedSerie = value)), sheetError = null)
    }

    fun onCreditNoteReason(value: String) = _uiState.update { state ->
        val sheet = state.sheet as? VoucherSheet.CreditNote ?: return@update state
        state.copy(
            sheet = sheet.copy(form = sheet.form.copy(reason = value.take(CashierValidation.MAX_REASON_LENGTH))),
            sheetError = null
        )
    }

    fun onCreditNoteAmount(value: String) = _uiState.update { state ->
        val sheet = state.sheet as? VoucherSheet.CreditNote ?: return@update state
        state.copy(sheet = sheet.copy(form = sheet.form.copy(amount = value)), sheetError = null)
    }

    fun onConfirmCreditNote() {
        val state = _uiState.value
        val sheet = state.sheet as? VoucherSheet.CreditNote ?: return
        val amount = sheet.form.parsedAmount ?: return
        if (!state.canIssueSunat || !state.isOnline || state.isSubmitting || !sheet.form.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, sheetError = null) }
        viewModelScope.launch {
            issueCreditNote(sheet.voucher.id, sheet.form.reason, amount, sheet.form.selectedSerie.ifBlank { null })
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            sheet = null,
                            isSubmitting = false,
                            notice = UiText.Resource(R.string.cashier_nc_done)
                        )
                    }
                    load()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, sheetError = e.toCashierText()) }
                }
        }
    }

    fun onRefreshSunat() {
        val state = _uiState.value
        val voucher = (state.sheet as? VoucherSheet.Detail)?.voucher ?: return
        val refreshUseCase = refreshVoucherStatus ?: return
        if (!state.isOnline || state.isRefreshingSunat || state.isSubmitting) return
        _uiState.update { it.copy(isRefreshingSunat = true, sheetError = null) }
        viewModelScope.launch {
            refreshUseCase(voucher.id)
                .onSuccess { updated ->
                    val noticeRes = when (updated.status) {
                        com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus.ACCEPTED_SUNAT ->
                            R.string.cashier_sunat_status_accepted_notice
                        com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus.REJECTED_SUNAT ->
                            R.string.cashier_sunat_status_rejected_notice
                        else -> R.string.cashier_sunat_status_pending_pse_notice
                    }
                    _uiState.update { current ->
                        current.copy(
                            isRefreshingSunat = false,
                            vouchers = current.vouchers.map { if (it.id == updated.id) updated else it },
                            sheet = VoucherSheet.Detail(updated),
                            notice = UiText.Resource(noticeRes)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRefreshingSunat = false, sheetError = e.toCashierText()) }
                }
        }
    }

    fun onOpenPdf() {
        val voucher = (_uiState.value.sheet as? VoucherSheet.Detail)?.voucher ?: return
        downloadFiscalFile(voucher.pdfUrl, "${voucher.serie}-${voucher.number}.pdf", "application/pdf", CheckoutDocumentActionMode.OPEN)
    }

    fun onSharePdf() {
        val voucher = (_uiState.value.sheet as? VoucherSheet.Detail)?.voucher ?: return
        downloadFiscalFile(voucher.pdfUrl, "${voucher.serie}-${voucher.number}.pdf", "application/pdf", CheckoutDocumentActionMode.SHARE)
    }

    fun onShareXml() {
        val voucher = (_uiState.value.sheet as? VoucherSheet.Detail)?.voucher ?: return
        downloadFiscalFile(voucher.xmlUrl, "${voucher.serie}-${voucher.number}.xml", "application/xml", CheckoutDocumentActionMode.SHARE)
    }

    fun onShareCdr() {
        val voucher = (_uiState.value.sheet as? VoucherSheet.Detail)?.voucher ?: return
        downloadFiscalFile(voucher.cdrUrl, "cdr-${voucher.serie}-${voucher.number}.zip", "application/zip", CheckoutDocumentActionMode.SHARE)
    }

    private fun downloadFiscalFile(
        url: String?,
        fileNamePrefix: String,
        mimeType: String,
        mode: CheckoutDocumentActionMode
    ) {
        val targetUrl = url?.takeIf { it.isNotBlank() } ?: return
        val downloader = downloadVoucherDocument ?: return
        val state = _uiState.value
        if (!state.isOnline || state.isDownloadingDocument) return
        _uiState.update { it.copy(isDownloadingDocument = true, sheetError = null) }
        viewModelScope.launch {
            downloader(targetUrl, fileNamePrefix)
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            isDownloadingDocument = false,
                            pendingDocument = PendingDocumentFile(file, mimeType, mode)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isDownloadingDocument = false, sheetError = e.toCashierText()) }
                }
        }
    }

    fun onDocumentHandled() = _uiState.update { it.copy(pendingDocument = null) }

    fun onDocumentOpenFailed() =
        _uiState.update { it.copy(pendingDocument = null, sheetError = UiText.Resource(R.string.cashier_exit_pass_no_app)) }

    fun onBackToDetail() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val voucher = when (val sheet = state.sheet) {
            is VoucherSheet.Void -> sheet.voucher
            is VoucherSheet.CreditNote -> sheet.voucher
            else -> return
        }
        _uiState.update { it.copy(sheet = VoucherSheet.Detail(voucher), sheetError = null) }
    }

    fun onDismissSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(sheet = null, sheetError = null) }
    }
}
