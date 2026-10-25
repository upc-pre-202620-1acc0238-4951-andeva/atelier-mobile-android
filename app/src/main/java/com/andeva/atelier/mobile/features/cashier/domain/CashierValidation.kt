package com.andeva.atelier.mobile.features.cashier.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Reglas de docs/api/invoicing.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object CashierValidation {
    val IGV_RATE: BigDecimal = BigDecimal("0.18")

    private const val RUC_LENGTH = 11
    private const val DNI_LENGTH = 8
    private const val SERIES_LENGTH = 4

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
}
