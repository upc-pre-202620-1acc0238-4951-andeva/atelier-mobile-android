package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Comprobante por emitir, armado en la app (no hay endpoint que facture una orden, H26). El [total] puede diferir en
 * céntimos del `totalAmount` de la orden: el saldo a cobrar sale siempre del comprobante emitido.
 */
data class VoucherDraft(
    val workOrderId: String,
    val customerId: String,
    val type: VoucherType,
    val customerInfo: CustomerFiscalInfo,
    val currency: String,
    val lines: List<VoucherLineDraft>
) {
    val total: BigDecimal
        get() = lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.quantity * line.unitPriceWithIgv }
            .setScale(2, RoundingMode.HALF_UP)
}

object VoucherDrafts {

    /**
     * Una línea `SERVICE` por tarea no cancelada y una `PRODUCT` por repuesto de esas tareas. [serviceNames] (por
     * `serviceId`) y [partNames] (por `productId`) vienen de los catálogos, porque el detalle trae nombres sin valor
     * real (H11). Un precio nulo o 0 falla con `lines`: el backend pide como mínimo 0.01.
     */
    fun fromWorkOrder(
        detail: WorkOrderDetail,
        serviceNames: Map<String, String>,
        partNames: Map<String, String>
    ): List<VoucherLineDraft> = detail.tasks
        .filter { it.status != TaskStatus.CANCELLED }
        .flatMap { task ->
            val service = VoucherLineDraft(
                itemType = VoucherItemType.SERVICE,
                itemId = task.serviceId,
                description = task.serviceId?.let { serviceNames[it] } ?: task.description,
                quantity = BigDecimal.ONE,
                unitPriceWithIgv = priced(task.price?.amount)
            )
            val parts = task.products.map { product ->
                VoucherLineDraft(
                    itemType = VoucherItemType.PRODUCT,
                    itemId = product.productId,
                    description = partNames[product.productId] ?: product.productName.orEmpty(),
                    quantity = product.quantity,
                    unitPriceWithIgv = priced(product.unitPrice?.amount)
                )
            }
            listOf(service) + parts
        }

    /** Tipo y datos fiscales sugeridos; lo que no encaja queda en boleta con CE para que la UI lo corrija (H27). */
    fun suggestFiscalInfo(customer: Customer): Pair<VoucherType, CustomerFiscalInfo> {
        val taxId = customer.taxId.orEmpty()
        val (type, document) = when {
            customer.type == CustomerType.COMPANY && CashierValidation.isRuc(taxId) ->
                VoucherType.FACTURA to FiscalDocumentType.RUC
            customer.type == CustomerType.INDIVIDUAL && CashierValidation.isDni(taxId) ->
                VoucherType.BOLETA to FiscalDocumentType.DNI
            else -> VoucherType.BOLETA to FiscalDocumentType.CE
        }
        return type to CustomerFiscalInfo(document, taxId, customer.displayName, fiscalAddress = null)
    }

    private fun priced(net: BigDecimal?): BigDecimal {
        if (net == null || net.signum() <= 0) throw InvalidCashierInputException("lines")
        return CashierValidation.withIgv(net)
    }
}
