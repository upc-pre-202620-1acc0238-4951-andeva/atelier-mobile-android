package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.MissingFiscalSeriesException
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class VouchersUseCasesTest {

    private val cashier = mockk<CashierRepository>()

    private fun pen(value: String) = Money(BigDecimal(value), Money.PEN)

    private fun voucher(
        id: String = "v1",
        type: VoucherType? = VoucherType.FACTURA,
        status: VoucherStatus? = VoucherStatus.ACCEPTED_SUNAT,
        total: String = "118.00",
        customerId: String? = "c1"
    ) = Voucher(
        id = id,
        branchId = "b1",
        customerId = customerId,
        workOrderId = "wo1",
        type = type,
        serie = if (type == VoucherType.FACTURA) "F001" else "B001",
        number = 10,
        subtotal = pen("100.00"),
        igvAmount = pen("18.00"),
        totalAmount = pen(total),
        paidAmount = pen("0.00"),
        pendingBalance = pen(total),
        paymentStatus = VoucherPaymentStatus.PENDING,
        status = status,
        customerInfo = null,
        pdfUrl = null,
        xmlUrl = null,
        cdrUrl = null,
        sunatResponseCode = "0",
        sunatDescription = "Aceptado",
        lines = emptyList(),
        payments = emptyList(),
        issuedAt = null,
        currency = Money.PEN
    )

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidCashierInputException).field

    @Test
    fun `GetVouchersUseCase delega filtros y devuelve la lista`() = runTest {
        val list = listOf(voucher())
        val from = LocalDate.of(2026, 11, 1)
        val to = LocalDate.of(2026, 11, 16)
        coEvery { cashier.getVouchers(VoucherType.FACTURA, VoucherStatus.ACCEPTED_SUNAT, from, to) } returns
            Result.success(list)

        val result = GetVouchersUseCase(cashier)(VoucherType.FACTURA, VoucherStatus.ACCEPTED_SUNAT, from, to)

        assertSame(list, result.getOrThrow())
    }

    @Test
    fun `VoidVoucherUseCase rechaza motivo vacio o mayor a 250 caracteres sin llamar al repositorio`() = runTest {
        val useCase = VoidVoucherUseCase(cashier)

        assertEquals("reason", field(useCase("v1", "   ")))
        assertEquals("reason", field(useCase("v1", "a".repeat(251))))
        coVerify(exactly = 0) { cashier.voidVoucher(any(), any()) }
    }

    @Test
    fun `VoidVoucherUseCase recorta el motivo y llama al repositorio`() = runTest {
        val voided = voucher(status = VoucherStatus.VOIDED)
        coEvery { cashier.voidVoucher("v1", "Error en RUC") } returns Result.success(voided)

        val result = VoidVoucherUseCase(cashier)("v1", "  Error en RUC  ")

        assertSame(voided, result.getOrThrow())
    }

    @Test
    fun `IssueCreditNoteUseCase valida motivo y monto antes de leer comprobante`() = runTest {
        val useCase = IssueCreditNoteUseCase(cashier)

        assertEquals("reason", field(useCase("v1", "   ", BigDecimal("10.00"))))
        assertEquals("reason", field(useCase("v1", "x".repeat(251), BigDecimal("10.00"))))
        assertEquals("amount", field(useCase("v1", "Descuento", BigDecimal.ZERO)))
        assertEquals("amount", field(useCase("v1", "Descuento", BigDecimal("10.123"))))
        coVerify(exactly = 0) { cashier.getVoucher(any()) }
    }

    @Test
    fun `IssueCreditNoteUseCase rechaza comprobante no vigente o monto mayor al total`() = runTest {
        val useCase = IssueCreditNoteUseCase(cashier)
        coEvery { cashier.getVoucher("v-void") } returns Result.success(voucher(id = "v-void", status = VoucherStatus.VOIDED))
        coEvery { cashier.getVoucher("v-draft") } returns Result.success(voucher(id = "v-draft", status = VoucherStatus.DRAFT))
        coEvery { cashier.getVoucher("v-nc") } returns Result.success(voucher(id = "v-nc", type = VoucherType.NOTA_CREDITO))
        coEvery { cashier.getVoucher("v1") } returns Result.success(voucher(total = "118.00"))

        assertEquals("status", field(useCase("v-void", "Motivo", BigDecimal("10.00"))))
        assertEquals("status", field(useCase("v-draft", "Motivo", BigDecimal("10.00"))))
        assertEquals("status", field(useCase("v-nc", "Motivo", BigDecimal("10.00"))))
        assertEquals("amount", field(useCase("v1", "Motivo", BigDecimal("118.01"))))
        coVerify(exactly = 0) { cashier.issueCreditNote(any(), any(), any(), any()) }
    }

    @Test
    fun `IssueCreditNoteUseCase falla con MissingFiscalSeriesException si no hay serie de nota de credito activa`() = runTest {
        coEvery { cashier.getVoucher("v1") } returns Result.success(voucher())
        coEvery { cashier.getBranchSeries() } returns Result.success(
            listOf(
                FiscalSeries("s1", "b1", VoucherType.FACTURA, "F001", 10, active = true),
                FiscalSeries("s2", "b1", VoucherType.NOTA_CREDITO, "FC01", 0, active = false)
            )
        )

        val result = IssueCreditNoteUseCase(cashier)("v1", "Descuento", BigDecimal("50.00"))

        assertTrue(result.exceptionOrNull() is MissingFiscalSeriesException)
    }

    @Test
    fun `IssueCreditNoteUseCase elige serie FC para factura y BC para boleta`() = runTest {
        val factura = voucher(id = "v-f", type = VoucherType.FACTURA)
        val boleta = voucher(id = "v-b", type = VoucherType.BOLETA)
        val nc = voucher(id = "v-nc", type = VoucherType.NOTA_CREDITO)
        coEvery { cashier.getVoucher("v-f") } returns Result.success(factura)
        coEvery { cashier.getVoucher("v-b") } returns Result.success(boleta)
        coEvery { cashier.getBranchSeries() } returns Result.success(
            listOf(
                FiscalSeries("s1", "b1", VoucherType.NOTA_CREDITO, "BC01", 1, active = true),
                FiscalSeries("s2", "b1", VoucherType.NOTA_CREDITO, "FC01", 1, active = true)
            )
        )
        coEvery { cashier.issueCreditNote(factura, "FC01", "Devolución", BigDecimal("50.00")) } returns Result.success(nc)
        coEvery { cashier.issueCreditNote(boleta, "BC01", "Devolución", BigDecimal("50.00")) } returns Result.success(nc)

        val useCase = IssueCreditNoteUseCase(cashier)
        assertSame(nc, useCase("v-f", "  Devolución  ", BigDecimal("50.00")).getOrThrow())
        assertSame(nc, useCase("v-b", "Devolución", BigDecimal("50.00")).getOrThrow())
    }
}
