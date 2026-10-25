package com.andeva.atelier.mobile.features.cashier.infrastructure.local

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.DailyCashSummary
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.Payment
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLine
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherSummary
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.domain.isActiveSale
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.CustomerFiscalInfoDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.DailyReconciliationDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.ElectronicVoucherDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.IssueVoucherRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.RegisterPaymentRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.SeriesConfigurationDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.VoucherLineDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.VoucherLineRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.VoucherPaymentDto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Room guarda los montos como texto decimal (docs/06-offline-sync.md §5). Un tipo, estado o forma de pago que la app no
 * conoce queda en null; las columnas obligatorias de la caché guardan entonces texto vacío. Sin monto no hay fila (null):
 * nunca se guarda un 0 inventado.
 */
fun ElectronicVoucherDto.toEntity(fallbackWorkOrderId: String?, syncedAt: Long): VoucherEntity? {
    val total = totalAmount ?: return null
    val paid = paidAmount ?: return null
    val pending = pendingBalance ?: return null
    return VoucherEntity(
        id = id,
        workOrderId = workOrderId ?: fallbackWorkOrderId,
        voucherType = voucherType.orEmpty(),
        serie = serie.orEmpty(),
        number = number ?: 0,
        totalAmount = total.toPlainString(),
        paidAmount = paid.toPlainString(),
        pendingBalance = pending.toPlainString(),
        paymentStatus = paymentStatus.orEmpty(),
        status = status.orEmpty(),
        pdfUrl = digitalReceipts?.pdfUrl,
        syncedAt = syncedAt
    )
}

/** null si algún monto guardado no se puede leer: nunca se muestra un 0 inventado. */
fun VoucherEntity.toDomain(): VoucherSummary? = VoucherSummary(
    id = id,
    workOrderId = workOrderId,
    type = VoucherType.from(voucherType),
    serie = serie,
    number = number,
    totalAmount = totalAmount.toBigDecimalOrNull() ?: return null,
    paidAmount = paidAmount.toBigDecimalOrNull() ?: return null,
    pendingBalance = pendingBalance.toBigDecimalOrNull() ?: return null,
    paymentStatus = paymentStatus.toEnum<VoucherPaymentStatus>(),
    status = status.toEnum<VoucherStatus>(),
    pdfUrl = pdfUrl
)

fun ElectronicVoucherDto.toDomain(): Voucher {
    val currency = currency ?: Money.PEN
    return Voucher(
        id = id,
        branchId = branchId,
        customerId = customerId,
        workOrderId = workOrderId,
        type = VoucherType.from(voucherType),
        serie = serie.orEmpty(),
        number = number ?: 0,
        subtotal = money(subtotal, currency),
        igvAmount = money(igvAmount, currency),
        totalAmount = money(totalAmount, currency),
        paidAmount = money(paidAmount, currency),
        pendingBalance = money(pendingBalance, currency),
        paymentStatus = paymentStatus.toEnum<VoucherPaymentStatus>(),
        status = status.toEnum<VoucherStatus>(),
        customerInfo = customerInfo?.toDomain(),
        pdfUrl = digitalReceipts?.pdfUrl,
        xmlUrl = digitalReceipts?.xmlUrl,
        cdrUrl = digitalReceipts?.cdrUrl,
        sunatResponseCode = sunatResponse?.responseCode,
        sunatDescription = sunatResponse?.description,
        lines = lines.orEmpty().map { it.toDomain() },
        payments = payments.orEmpty().map { it.toDomain(currency) },
        issuedAt = issuedAt.toInstant(),
        currency = this.currency
    )
}

fun CustomerFiscalInfoDto.toDomain() = CustomerFiscalInfo(
    documentType = documentType.toEnum<FiscalDocumentType>(),
    taxId = taxId.orEmpty(),
    legalName = legalName.orEmpty(),
    fiscalAddress = fiscalAddress
)

fun VoucherLineDto.toDomain() = VoucherLine(
    id = id,
    itemId = itemId,
    itemType = itemType.toEnum<VoucherItemType>(),
    description = description.orEmpty(),
    quantity = quantity ?: BigDecimal.ZERO,
    unitValue = unitValue,
    unitPrice = unitPrice,
    igvAmount = igvAmount,
    totalLine = totalLine
)

fun VoucherPaymentDto.toDomain(fallbackCurrency: String = Money.PEN) = Payment(
    id = id,
    voucherId = voucherId,
    amount = money(amount, currency ?: fallbackCurrency),
    method = paymentMethod.toEnum<PaymentMethod>(),
    transactionReference = transactionReference,
    status = status,
    paidAt = paidAt.toInstant()
)

fun SeriesConfigurationDto.toDomain() = FiscalSeries(
    id = id,
    branchId = branchId,
    type = VoucherType.from(voucherType),
    serie = serie.orEmpty(),
    currentCorrelative = currentCorrelative ?: 0,
    active = active ?: false
)

/** `voucherType` sale como código SUNAT; `unitPriceWithIgv` ya viene con IGV desde el borrador. */
fun VoucherDraft.toRequest(branchId: String, serie: String) = IssueVoucherRequestDto(
    branchId = branchId,
    currency = currency,
    customerId = customerId,
    customerInfo = CustomerFiscalInfoDto(
        documentType = customerInfo.documentType?.name,
        taxId = customerInfo.taxId,
        legalName = customerInfo.legalName,
        fiscalAddress = customerInfo.fiscalAddress
    ),
    lines = lines.map {
        VoucherLineRequestDto(
            description = it.description,
            itemType = it.itemType.name,
            quantity = it.quantity,
            unitPriceWithIgv = it.unitPriceWithIgv,
            itemId = it.itemId
        )
    },
    serie = serie,
    voucherType = type.code,
    workOrderId = workOrderId
)

/**
 * Solo la moneda cae a PEN si no se conoce; el monto nunca se inventa: un monto ausente o inválido (negativo) hace fallar el
 * mapeo, porque un saldo en 0 de más haría ver pagada una orden que no lo está.
 */
private fun money(amount: BigDecimal?, currency: String): Money {
    val known = if (currency == Money.PEN || currency == Money.USD) currency else Money.PEN
    return Money(requireNotNull(amount) { "Missing amount" }, known)
}

/** Si el comprobante cuenta como venta vigente, sin construir el modelo completo. */
fun ElectronicVoucherDto.isActiveSale() = isActiveSale(VoucherType.from(voucherType), status.toEnum<VoucherStatus>())

private fun String?.toInstant(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

private inline fun <reified T : Enum<T>> String?.toEnum(): T? = enumValues<T>().firstOrNull { it.name == this }

/** Una forma de pago que la app no conoce se omite del desglose; el total y los cobros llegan completos. */
fun DailyReconciliationDto.toDomain(requestedDate: LocalDate): DailyCashSummary {
    val currency = currency ?: Money.PEN
    return DailyCashSummary(
        date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: requestedDate,
        totalCollected = money(totalCollected, currency),
        paymentCount = paymentCount ?: payments.orEmpty().size,
        byMethod = breakdownByMethod.orEmpty().mapNotNull { (key, amount) ->
            key.toEnum<PaymentMethod>()?.let { it to money(amount, currency) }
        }.toMap(),
        payments = payments.orEmpty().map { it.toDomain(currency) }
    )
}

/** La moneda y el comprobante salen del comprobante leído; la sede, de la sesión. */
fun paymentRequest(
    voucher: Voucher,
    currency: String,
    branchId: String,
    amount: BigDecimal,
    method: PaymentMethod,
    transactionReference: String?
) = RegisterPaymentRequestDto(
    amount = amount,
    branchId = branchId,
    currency = currency,
    paymentMethod = method.name,
    voucherId = voucher.id,
    transactionReference = transactionReference
)
