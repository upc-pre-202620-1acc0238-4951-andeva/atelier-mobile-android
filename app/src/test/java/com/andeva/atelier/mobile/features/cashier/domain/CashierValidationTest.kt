package com.andeva.atelier.mobile.features.cashier.domain

import java.math.BigDecimal
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class CashierValidationTest {

    @Test
    fun `withIgv redondea a centimos`() {
        assertEquals(BigDecimal("118.00"), CashierValidation.withIgv(BigDecimal("100.00")))
        assertEquals(BigDecimal("0.12"), CashierValidation.withIgv(BigDecimal("0.10")))
        assertEquals(BigDecimal("39.33"), CashierValidation.withIgv(BigDecimal("33.33")))
    }

    @Test
    fun `factura exige RUC de 11 digitos`() {
        assertTrue(CashierValidation.isRuc("20123456789"))
        assertFalse(CashierValidation.isRuc("2012345678"))
        assertFalse(CashierValidation.isRuc("2012345678a"))
        assertFalse(CashierValidation.isRuc("12345678"))
    }

    @Test
    fun `boleta acepta DNI de 8`() {
        assertTrue(CashierValidation.isDni("12345678"))
        assertFalse(CashierValidation.isDni("1234567"))
        assertFalse(CashierValidation.isDni("1234567a"))
    }

    @Test
    fun `serie F001 vale para factura y B001 para boleta`() {
        assertTrue(CashierValidation.isSeriesFor(VoucherType.FACTURA, "F001"))
        assertTrue(CashierValidation.isSeriesFor(VoucherType.BOLETA, "B001"))
        assertFalse(CashierValidation.isSeriesFor(VoucherType.FACTURA, "B001"))
        assertFalse(CashierValidation.isSeriesFor(VoucherType.BOLETA, "F001"))
        assertFalse(CashierValidation.isSeriesFor(VoucherType.FACTURA, "F01"))
        assertFalse(CashierValidation.isSeriesFor(VoucherType.FACTURA, "F0012"))
        assertFalse(CashierValidation.isSeriesFor(VoucherType.NOTA_CREDITO, "F001"))
    }

    @Test
    fun `VoucherType acepta codigo o nombre`() {
        assertEquals(VoucherType.FACTURA, VoucherType.from("01"))
        assertEquals(VoucherType.BOLETA, VoucherType.from("BOLETA"))
        assertEquals(VoucherType.NOTA_CREDITO, VoucherType.from("07"))
        assertNull(VoucherType.from("99"))
        assertNull(VoucherType.from(null))
    }

    @Test
    fun `isActiveSale excluye anulados y notas de credito`() {
        assertFalse(summary(VoucherType.FACTURA, VoucherStatus.VOIDED).isActiveSale)
        assertFalse(summary(VoucherType.NOTA_CREDITO, VoucherStatus.ISSUED).isActiveSale)
        assertTrue(summary(VoucherType.FACTURA, VoucherStatus.ISSUED).isActiveSale)
    }

    private fun summary(type: VoucherType, status: VoucherStatus) = VoucherSummary(
        id = "v1", workOrderId = "w1", type = type, serie = "F001", number = 1,
        totalAmount = BigDecimal("118.00"), paidAmount = BigDecimal.ZERO,
        pendingBalance = BigDecimal("118.00"), paymentStatus = VoucherPaymentStatus.PENDING,
        status = status, pdfUrl = null
    )
}
