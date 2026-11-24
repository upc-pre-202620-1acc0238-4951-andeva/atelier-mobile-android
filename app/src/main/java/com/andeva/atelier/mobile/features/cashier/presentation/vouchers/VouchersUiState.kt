package com.andeva.atelier.mobile.features.cashier.presentation.vouchers

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.CreditNoteForm
import com.andeva.atelier.mobile.features.cashier.presentation.VoidForm
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.PendingDocumentFile

/** Filtros de la lista de comprobantes emitidos. */
enum class VoucherFilter { ALL, ACCEPTED, WITH_BALANCE, VOIDED, CREDIT_NOTES }

sealed interface VoucherSheet {
    data class Detail(val voucher: Voucher) : VoucherSheet
    data class Void(val voucher: Voucher, val form: VoidForm) : VoucherSheet
    data class CreditNote(val voucher: Voucher, val form: CreditNoteForm) : VoucherSheet
}

/** Comprobantes SUNAT emitidos en la sede: boletas, facturas y notas de crédito. */
data class VouchersUiState(
    val vouchers: List<Voucher> = emptyList(),
    val filter: VoucherFilter = VoucherFilter.ALL,
    val hasLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = false,
    val canIssueSunat: Boolean = false,
    val sheet: VoucherSheet? = null,
    val isSubmitting: Boolean = false,
    val isRefreshingSunat: Boolean = false,
    val isDownloadingDocument: Boolean = false,
    val pendingDocument: PendingDocumentFile? = null,
    val sheetError: UiText? = null,
    val notice: UiText? = null
) {
    val showSkeleton: Boolean get() = !hasLoaded && vouchers.isEmpty() && error == null

    val filteredVouchers: List<Voucher>
        get() = when (filter) {
            VoucherFilter.ALL -> vouchers
            VoucherFilter.ACCEPTED -> vouchers.filter {
                it.status == VoucherStatus.ACCEPTED_SUNAT || it.status == VoucherStatus.ISSUED
            }
            VoucherFilter.WITH_BALANCE -> vouchers.filter { it.isActiveSale && it.pendingBalance.amount.signum() > 0 }
            VoucherFilter.VOIDED -> vouchers.filter { it.status == VoucherStatus.VOIDED }
            VoucherFilter.CREDIT_NOTES -> vouchers.filter { it.type == VoucherType.NOTA_CREDITO }
        }
}

/** Solo una factura o boleta vigente no borrador admite anulación o nota de crédito. */
internal val Voucher.canIssueActions: Boolean
    get() = isActiveSale && status != VoucherStatus.DRAFT
