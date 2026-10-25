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
     * real (H11); si faltan, la línea queda con un nombre que pide editarla ([VoucherLineDraft.needsDescription]). Una
     * tarea o repuesto en 0 no da línea (no cambia el total y el backend pide como mínimo 0.01). Un precio nulo o
     * negativo, o no tener ninguna línea, falla con `lines`.
     */
    fun fromWorkOrder(
        detail: WorkOrderDetail,
        serviceNames: Map<String, String>,
        partNames: Map<String, String>
    ): List<VoucherLineDraft> {
        val lines = billableTasks(detail).flatMap { task ->
            val service = priced(task.price?.amount)?.let { price ->
                VoucherLineDraft(
                    itemType = VoucherItemType.SERVICE,
                    itemId = task.serviceId,
                    description = task.serviceId?.let { serviceNames[it] } ?: task.description,
                    quantity = BigDecimal.ONE,
                    unitPriceWithIgv = price
                )
            }
            val parts = task.products.mapNotNull { product ->
                priced(product.unitPrice?.amount)?.let { price ->
                    VoucherLineDraft(
                        itemType = VoucherItemType.PRODUCT,
                        itemId = product.productId,
                        description = partNames[product.productId] ?: product.productName.orEmpty(),
                        quantity = product.quantity,
                        unitPriceWithIgv = price
                    )
                }
            }
            listOfNotNull(service) + parts
        }
        if (lines.isEmpty()) throw InvalidCashierInputException("lines")
        return lines
    }

    /** Monedas de los precios que dan línea en [fromWorkOrder]: tareas no canceladas y sus repuestos, distintos de 0. */
    fun currenciesOf(detail: WorkOrderDetail): Set<String> = billableTasks(detail)
        .flatMap { task -> listOf(task.price) + task.products.map { it.unitPrice } }
        .filterNotNull()
        .filter { it.amount.signum() != 0 }
        .mapTo(mutableSetOf()) { it.currency }

    private fun billableTasks(detail: WorkOrderDetail) = detail.tasks.filter { it.status != TaskStatus.CANCELLED }

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

    /** Precio con IGV, o null si es 0 (sin línea). */
    private fun priced(net: BigDecimal?): BigDecimal? {
        if (net == null || net.signum() < 0) throw InvalidCashierInputException("lines")
        return if (net.signum() == 0) null else CashierValidation.withIgv(net)
    }
}
