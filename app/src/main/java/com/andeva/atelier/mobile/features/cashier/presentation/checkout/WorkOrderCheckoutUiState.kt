package com.andeva.atelier.mobile.features.cashier.presentation.checkout

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.Payment
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLineDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherSummary
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/** Qué acción de la pantalla está en vuelo; deshabilita las demás y pone el botón en carga. */
enum class CheckoutAction { Preparing, Issuing, Paying, MarkingPaid, ExitPass, Delivering }

/** Lo que muestra la pantalla de un comprobante, venga de la red (con IGV desglosado) o de la caché Room. */
data class VoucherView(
    val id: String,
    val type: VoucherType?,
    val serie: String,
    val number: Int,
    val currency: String?,
    /** Subtotal e IGV tal como los da el backend; null si solo hay la cabecera en caché. */
    val subtotal: BigDecimal?,
    val igv: BigDecimal?,
    val total: BigDecimal,
    val paid: BigDecimal,
    val pending: BigDecimal,
    val paymentStatus: VoucherPaymentStatus?,
    val status: VoucherStatus?,
    val isActiveSale: Boolean
)

fun Voucher.toView() = VoucherView(
    id = id,
    type = type,
    serie = serie,
    number = number,
    currency = currency,
    subtotal = subtotal.amount,
    igv = igvAmount.amount,
    total = totalAmount.amount,
    paid = paidAmount.amount,
    pending = pendingBalance.amount,
    paymentStatus = paymentStatus,
    status = status,
    isActiveSale = isActiveSale
)

fun VoucherSummary.toView() = VoucherView(
    id = id,
    type = type,
    serie = serie,
    number = number,
    currency = null,
    subtotal = null,
    igv = null,
    total = totalAmount,
    paid = paidAmount,
    pending = pendingBalance,
    paymentStatus = paymentStatus,
    status = status,
    isActiveSale = isActiveSale
)

/**
 * Comprobante en edición antes de emitir. El cajero corrige el tipo, los datos fiscales y el nombre de las líneas que
 * el detalle de la orden no trae con valor real (H11); los precios no se tocan.
 */
data class IssueForm(
    val base: VoucherDraft,
    val type: VoucherType,
    val documentType: FiscalDocumentType,
    val taxId: String,
    val legalName: String,
    val address: String,
    val lines: List<VoucherLineDraft>
) {
    val isCompany: Boolean get() = type == VoucherType.FACTURA

    fun toDraft(): VoucherDraft = base.copy(
        type = type,
        customerInfo = CustomerFiscalInfo(documentType, taxId.trim(), legalName.trim(), address.trim().ifBlank { null }),
        lines = lines
    )

    val total: BigDecimal get() = toDraft().total

    /** El total lleva IGV: la base imponible es total ÷ 1.18 y el IGV, lo que sobra. */
    val subtotal: BigDecimal
        get() = total.divide(BigDecimal.ONE + CashierValidation.IGV_RATE, 2, RoundingMode.HALF_UP)

    val igv: BigDecimal get() = total - subtotal

    val taxIdValid: Boolean
        get() {
            val id = taxId.trim()
            return when (documentType) {
                FiscalDocumentType.RUC -> CashierValidation.isRuc(id)
                FiscalDocumentType.DNI -> CashierValidation.isDni(id)
                FiscalDocumentType.CE, FiscalDocumentType.PASAPORTE -> id.length in 4..20
            }
        }

    val nameValid: Boolean get() = legalName.isNotBlank()
    val linesValid: Boolean get() = lines.none { it.needsDescription }
    val canSubmit: Boolean get() = taxIdValid && nameValid && linesValid

    /** Factura: siempre RUC. Boleta: el documento sugerido, o DNI si venía de factura. */
    fun withType(newType: VoucherType): IssueForm = copy(
        type = newType,
        documentType = when {
            newType == VoucherType.FACTURA -> FiscalDocumentType.RUC
            documentType == FiscalDocumentType.RUC -> FiscalDocumentType.DNI
            else -> documentType
        }
    )
}

/** Cobro en edición. [amount] es texto para no perder lo que el cajero escribe. */
data class PaymentForm(
    val voucher: VoucherView,
    val amount: String,
    val method: PaymentMethod = PaymentMethod.CASH,
    val reference: String = ""
) {
    /** El monto escrito, o null si no es un número con hasta 2 decimales entre 0.01 y el saldo. */
    val parsedAmount: BigDecimal?
        get() = amount.trim().replace(',', '.').toBigDecimalOrNull()?.takeIf {
            it >= MIN_PAYMENT && it.stripTrailingZeros().scale() <= 2 && it <= voucher.pending
        }

    val canSubmit: Boolean get() = parsedAmount != null && reference.trim().length <= MAX_REFERENCE

    companion object {
        val MIN_PAYMENT = BigDecimal("0.01")
        const val MAX_REFERENCE = 100
    }
}

sealed interface CheckoutSheet {
    data class Issue(val form: IssueForm) : CheckoutSheet
    data class Payment(val form: PaymentForm) : CheckoutSheet
    data object Deliver : CheckoutSheet
}

/**
 * "Cobrar y entregar": una orden `COMPLETED` pasa por comprobante, cobros, pase de salida y entrega. Todo es en línea
 * (docs/06-offline-sync.md); sin red solo se ve lo que hay en Room.
 */
data class WorkOrderCheckoutUiState(
    val workOrderId: String = "",
    val order: WorkOrder? = null,
    val cachedVouchers: List<VoucherSummary> = emptyList(),
    val liveVouchers: List<Voucher>? = null,
    val payments: List<Payment> = emptyList(),
    val hasLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    /** Falla de la lectura (no de una acción). */
    val error: UiText? = null,
    val sheet: CheckoutSheet? = null,
    val busy: CheckoutAction? = null,
    /** Error de la hoja abierta; se ve dentro de ella. */
    val sheetError: UiText? = null,
    /** Error de una acción sin hoja (preparar, marcar pagada, pase de salida). */
    val actionError: UiText? = null,
    val notice: UiText? = null,
    val delivered: Boolean = false,
    val exitPass: File? = null,
    val canReadInvoices: Boolean = false,
    val canIssueVoucher: Boolean = false,
    val canRegisterPayment: Boolean = false,
    val canIssueExitPass: Boolean = false,
    val canDeliver: Boolean = false
) {
    val vouchers: List<VoucherView>
        get() = liveVouchers?.map { it.toView() } ?: cachedVouchers.map { it.toView() }

    val activeVoucher: VoucherView? get() = vouchers.firstOrNull { it.isActiveSale }

    val isCompleted: Boolean get() = order?.status == WorkOrderStatus.COMPLETED
    val isOrderPaid: Boolean get() = order?.status == WorkOrderStatus.PAID

    /** El comprobante está saldado pero la orden todavía no figura pagada. */
    val canMarkPaid: Boolean
        get() = canRegisterPayment && !isOrderPaid && activeVoucher?.paymentStatus == VoucherPaymentStatus.PAID

    val hasAnyAccess: Boolean
        get() = canReadInvoices || canIssueVoucher || canRegisterPayment || canIssueExitPass || canDeliver

    /** Sin comprobantes en pantalla y todavía leyéndolos: esqueleto. Con caché se pinta Room y basta la barra. */
    val showSkeleton: Boolean get() = !hasLoaded && vouchers.isEmpty() && error == null
}
