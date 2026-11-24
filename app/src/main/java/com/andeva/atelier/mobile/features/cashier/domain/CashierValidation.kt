package com.andeva.atelier.mobile.features.cashier.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Reglas de docs/api/invoicing.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object CashierValidation {
    val IGV_RATE: BigDecimal = BigDecimal("0.18")

    private const val RUC_LENGTH = 11
    private const val DNI_LENGTH = 8
    private const val SERIES_LENGTH = 4
    const val MAX_REASON_LENGTH = 250
    const val MAX_LINE_DESCRIPTION_LENGTH = 200
    val MIN_AMOUNT: BigDecimal = BigDecimal("0.01")
    const val MONEY_SCALE = 2

    /** Nombres genéricos de `GET /work-orders/{id}` (H11): "Standard Service" y "Product {uuid}". */
    private const val SERVICE_PLACEHOLDER = "Standard Service"
    private val PRODUCT_PLACEHOLDER =
        Regex("Product [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    /** Una descripción vacía o con un nombre genérico del detalle de la orden no sirve para un comprobante. */
    fun isPlaceholderDescription(description: String): Boolean {
        val text = description.trim()
        return text.isEmpty() || text == SERVICE_PLACEHOLDER || PRODUCT_PLACEHOLDER.matches(text)
    }

    /** Pasa un precio sin IGV a uno con IGV: × 1.18, 2 decimales, HALF_UP. */
    fun withIgv(net: BigDecimal): BigDecimal =
        net.multiply(BigDecimal.ONE.add(IGV_RATE)).setScale(2, RoundingMode.HALF_UP)

    fun isRuc(value: String) = value.length == RUC_LENGTH && value.all { it in '0'..'9' }

    fun isDni(value: String) = value.length == DNI_LENGTH && value.all { it in '0'..'9' }

    /** `F` + 3 caracteres alfanuméricos para factura, `B` + 3 para boleta. */
    fun isSeriesFor(type: VoucherType, serie: String): Boolean {
        val prefix = when (type) {
            VoucherType.FACTURA -> 'F'
            VoucherType.BOLETA -> 'B'
            VoucherType.NOTA_CREDITO -> return false
        }
        return serie.length == SERIES_LENGTH && serie[0] == prefix &&
            serie.drop(1).all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' }
    }

    /** Serie de 4 caracteres alfanuméricos para nota de crédito (`F` para factura, `B` para boleta). */
    fun isCreditNoteSeries(serie: String, referenceType: VoucherType? = null): Boolean {
        val trimmed = serie.trim()
        if (trimmed.length != SERIES_LENGTH) return false
        val first = trimmed[0]
        val validPrefix = when (referenceType) {
            VoucherType.FACTURA -> first == 'F'
            VoucherType.BOLETA -> first == 'B'
            else -> first == 'F' || first == 'B'
        }
        return validPrefix && trimmed.drop(1).all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' }
    }

    /** Motivo obligatorio para comunicación de baja o nota de crédito (1..250 caracteres). */
    fun isValidReason(reason: String): Boolean = reason.trim().length in 1..MAX_REASON_LENGTH

    /** Importe monetario positivo con hasta 2 decimales y, si se indica, sin superar [max]. */
    fun isValidMoneyAmount(amount: BigDecimal, max: BigDecimal? = null): Boolean =
        amount >= MIN_AMOUNT &&
            amount.stripTrailingZeros().scale() <= MONEY_SCALE &&
            (max == null || amount <= max)
}
