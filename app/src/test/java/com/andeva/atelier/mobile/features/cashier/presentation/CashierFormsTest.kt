package com.andeva.atelier.mobile.features.cashier.presentation

import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLineDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.CreditNoteForm
import com.andeva.atelier.mobile.features.cashier.presentation.VoidForm
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.IssueForm
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.PaymentForm
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.VoucherView
import com.andeva.atelier.mobile.features.cashier.presentation.series.SeriesForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CashierFormsTest {

    private fun line(description: String, price: String, quantity: String = "1") = VoucherLineDraft(
        itemType = VoucherItemType.SERVICE,
        itemId = null,
        description = description,
        quantity = BigDecimal(quantity),
        unitPriceWithIgv = BigDecimal(price)
    )

    private fun form(lines: List<VoucherLineDraft>, type: VoucherType = VoucherType.BOLETA, taxId: String = "45871236") =
        IssueForm(
            base = VoucherDraft(
                workOrderId = "wo",
                customerId = "c",
                type = type,
                customerInfo = CustomerFiscalInfo(FiscalDocumentType.DNI, taxId, "Rosa Quispe", null),
                currency = "PEN",
                lines = lines
            ),
            type = type,
            documentType = FiscalDocumentType.DNI,
            taxId = taxId,
            legalName = "Rosa Quispe",
            address = "",
            lines = lines
        )

    @Test
    fun `el IGV se desglosa del total con 18 por ciento`() {
        val form = form(listOf(line("Afinamiento", "1180.00")))
        assertEquals(BigDecimal("1000.00"), form.subtotal)
        assertEquals(BigDecimal("180.00"), form.igv)
        assertEquals(BigDecimal("1180.00"), form.total)
    }

    @Test
    fun `no se emite con una linea sin nombre real`() {
        assertFalse(form(listOf(line("Standard Service", "100.00"))).canSubmit)
        val named = form(listOf(line("Standard Service", "100.00"))).copy(lines = listOf(line("Cambio de aceite", "100.00")))
        assertTrue(named.canSubmit)
    }

    @Test
    fun `la factura pide RUC de 11 digitos y pasar a boleta vuelve a DNI`() {
        val factura = form(listOf(line("Servicio", "100.00"))).withType(VoucherType.FACTURA)
        assertEquals(FiscalDocumentType.RUC, factura.documentType)
        assertFalse(factura.copy(taxId = "45871236").canSubmit)
        assertTrue(factura.copy(taxId = "20512345678").canSubmit)
        assertEquals(FiscalDocumentType.DNI, factura.withType(VoucherType.BOLETA).documentType)
    }

    private fun voucher(pending: String) = VoucherView(
        id = "v", type = VoucherType.BOLETA, serie = "B001", number = 1, currency = "PEN",
        subtotal = null, igv = null, total = BigDecimal("1200.00"), paid = BigDecimal("500.00"),
        pending = BigDecimal(pending), paymentStatus = VoucherPaymentStatus.PARTIAL,
        status = VoucherStatus.ISSUED, isActiveSale = true
    )

    @Test
    fun `el cobro acepta hasta el saldo con dos decimales`() {
        val form = PaymentForm(voucher("700.00"), "700.00")
        assertEquals(BigDecimal("700.00"), form.parsedAmount)
        assertEquals(BigDecimal("350.50"), form.copy(amount = "350,50").parsedAmount)
        assertNull(form.copy(amount = "700.01").parsedAmount)
        assertNull(form.copy(amount = "0").parsedAmount)
        assertNull(form.copy(amount = "10.123").parsedAmount)
        assertFalse(form.copy(amount = "abc").canSubmit)
        assertTrue(form.canSubmit)
        assertFalse(form.copy(method = com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod.DIGITAL_WALLET_YAPE, reference = "   ").canSubmit)
        assertTrue(form.copy(method = com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod.DIGITAL_WALLET_YAPE, reference = "OP-12345").canSubmit)
    }

    @Test
    fun `la serie debe empezar con F para factura y B para boleta`() {
        assertTrue(SeriesForm(VoucherType.FACTURA, "F001").canSubmit)
        assertFalse(SeriesForm(VoucherType.FACTURA, "B001").canSubmit)
        assertTrue(SeriesForm(VoucherType.BOLETA, "B01A", initial = "25").canSubmit)
        assertEquals(25, SeriesForm(initial = "25").initialCorrelative)
        assertFalse(SeriesForm(VoucherType.BOLETA, "B001", initial = "x").canSubmit)
    }

    @Test
    fun `anular exige motivo de hasta 250 caracteres`() {
        assertFalse(VoidForm("v1", "B001-00000001", reason = "   ").canSubmit)
        assertTrue(VoidForm("v1", "B001-00000001", reason = "Error en datos").canSubmit)
        assertFalse(VoidForm("v1", "B001-00000001", reason = "x".repeat(251)).canSubmit)
    }

    @Test
    fun `nota de credito exige motivo y monto valido hasta el total`() {
        val base = CreditNoteForm(
            referenceVoucherId = "v1",
            referenceLabel = "F001-00000001",
            maxAmount = BigDecimal("500.00"),
            currency = "PEN",
            reason = "Descuento por demora",
            amount = "500.00"
        )
        assertTrue(base.canSubmit)
        assertEquals(BigDecimal("125.50"), base.copy(amount = "125,50").parsedAmount)
        assertNull(base.copy(amount = "500.01").parsedAmount)
        assertNull(base.copy(amount = "0").parsedAmount)
        assertNull(base.copy(amount = "10.123").parsedAmount)
        assertFalse(base.copy(reason = "  ").canSubmit)
    }
}
