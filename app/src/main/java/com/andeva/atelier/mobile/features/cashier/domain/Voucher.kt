package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.time.Instant

/**
 * Una venta vigente es una factura o boleta con validez fiscal: ni anulada ni rechazada por SUNAT (esa se puede volver
 * a emitir). Un borrador sí cuenta. Las notas de crédito no cuentan.
 */
internal fun isActiveSale(type: VoucherType?, status: VoucherStatus?): Boolean =
    (type == VoucherType.FACTURA || type == VoucherType.BOLETA) &&
        status != VoucherStatus.VOIDED && status != VoucherStatus.REJECTED_SUNAT

/** `ElectronicVoucherResource` (docs/api/invoicing.md). */
data class Voucher(
    val id: String,
    val branchId: String?,
    val customerId: String?,
    val workOrderId: String?,
    val type: VoucherType?,
    val serie: String,
    val number: Int,
    val subtotal: Money,
    val igvAmount: Money,
    val totalAmount: Money,
    val paidAmount: Money,
    val pendingBalance: Money,
    val paymentStatus: VoucherPaymentStatus?,
    val status: VoucherStatus?,
    val customerInfo: CustomerFiscalInfo?,
    val pdfUrl: String?,
    val xmlUrl: String?,
    val cdrUrl: String?,
    val sunatResponseCode: String?,
    val sunatDescription: String?,
    val lines: List<VoucherLine>,
    val payments: List<Payment>,
    val issuedAt: Instant?,
    /** Moneda tal como la envió el backend; null si falta. Los montos usan PEN si no es PEN ni USD. */
    val currency: String? = null
) {
    val isActiveSale: Boolean get() = isActiveSale(type, status)
}

/** Cabecera de un comprobante, lo único que guarda la caché Room `vouchers`. */
data class VoucherSummary(
    val id: String,
    val workOrderId: String?,
    val type: VoucherType?,
    val serie: String,
    val number: Int,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val pendingBalance: BigDecimal,
    val paymentStatus: VoucherPaymentStatus?,
    val status: VoucherStatus?,
    val pdfUrl: String?
) {
    val isActiveSale: Boolean get() = isActiveSale(type, status)
}

/** `CustomerFiscalInfoResponse` / `CustomerFiscalInfoRequest`. */
data class CustomerFiscalInfo(
    val documentType: FiscalDocumentType?,
    val taxId: String,
    val legalName: String,
    val fiscalAddress: String?
)

/** `VoucherLineResource`: [unitPrice] y [totalLine] ya incluyen IGV según el backend. */
data class VoucherLine(
    val id: String?,
    val itemId: String?,
    val itemType: VoucherItemType?,
    val description: String,
    val quantity: BigDecimal,
    val unitValue: BigDecimal?,
    val unitPrice: BigDecimal?,
    val igvAmount: BigDecimal?,
    val totalLine: BigDecimal?
)

/** `VoucherLineRequest`: `unitPriceWithIgv` lleva IGV; los precios de la orden no (docs/api/README.md §6). */
data class VoucherLineDraft(
    val itemType: VoucherItemType,
    val itemId: String?,
    val description: String,
    val quantity: BigDecimal,
    val unitPriceWithIgv: BigDecimal
) {
    /**
     * La descripción está vacía o es un nombre genérico del detalle de la orden (H11): la UI debe pedir editarla antes
     * de emitir. Pasa con los repuestos del Cajero, que no tiene `inventory:parts:read`.
     */
    val needsDescription: Boolean get() = CashierValidation.isPlaceholderDescription(description)
}
