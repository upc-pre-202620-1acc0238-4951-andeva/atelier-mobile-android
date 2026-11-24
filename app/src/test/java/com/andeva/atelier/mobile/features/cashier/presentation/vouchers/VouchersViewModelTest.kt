package com.andeva.atelier.mobile.features.cashier.presentation.vouchers

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.cashier.application.GetVouchersUseCase
import com.andeva.atelier.mobile.features.cashier.application.IssueCreditNoteUseCase
import com.andeva.atelier.mobile.features.cashier.application.VoidVoucherUseCase
import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

class VouchersViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<CashierRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private fun pen(amount: String) = Money(BigDecimal(amount), Money.PEN)

    private fun voucher(
        id: String,
        type: VoucherType = VoucherType.FACTURA,
        status: VoucherStatus = VoucherStatus.ACCEPTED_SUNAT,
        total: String = "118.00",
        pending: String = "0.00",
        issuedAt: Instant = Instant.parse("2026-11-16T15:00:00Z")
    ) = Voucher(
        id = id,
        branchId = "b1",
        customerId = "c1",
        workOrderId = "wo1",
        type = type,
        serie = when (type) {
            VoucherType.FACTURA -> "F001"
            VoucherType.BOLETA -> "B001"
            VoucherType.NOTA_CREDITO -> "FC01"
        },
        number = 10,
        subtotal = pen("100.00"),
        igvAmount = pen("18.00"),
        totalAmount = pen(total),
        paidAmount = pen((BigDecimal(total) - BigDecimal(pending)).toPlainString()),
        pendingBalance = pen(pending),
        paymentStatus = if (BigDecimal(pending).signum() > 0) VoucherPaymentStatus.PARTIAL else VoucherPaymentStatus.PAID,
        status = status,
        customerInfo = null,
        pdfUrl = null,
        xmlUrl = null,
        cdrUrl = null,
        sunatResponseCode = "0",
        sunatDescription = "Aceptado",
        lines = emptyList(),
        payments = emptyList(),
        issuedAt = issuedAt,
        currency = Money.PEN
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u1",
        email = "cajero@taller.pe",
        fullName = "Cajero",
        tenantId = "t1",
        tenantName = "Taller Suárez",
        membershipId = "m1",
        permissions = permissions.map { it.value }.toSet(),
        plan = null,
        branchId = "b1"
    )

    private fun createViewModel(
        permissions: List<Permission> = listOf(Permission.INVOICES_READ, Permission.INVOICES_ISSUE_SUNAT)
    ): VouchersViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        return VouchersViewModel(
            getVouchers = GetVouchersUseCase(repository),
            voidVoucher = VoidVoucherUseCase(repository),
            issueCreditNote = IssueCreditNoteUseCase(repository),
            observeSession = observeSession,
            connectivity = connectivity
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `carga comprobantes ordenados por fecha y aplica filtros`() = runTest(mainRule.dispatcher) {
        val vAcceptedWithBalance = voucher(
            id = "v1",
            status = VoucherStatus.ACCEPTED_SUNAT,
            pending = "50.00",
            issuedAt = Instant.parse("2026-11-16T10:00:00Z")
        )
        val vVoided = voucher(
            id = "v2",
            status = VoucherStatus.VOIDED,
            issuedAt = Instant.parse("2026-11-16T12:00:00Z")
        )
        val vCreditNote = voucher(
            id = "v3",
            type = VoucherType.NOTA_CREDITO,
            status = VoucherStatus.ACCEPTED_SUNAT,
            issuedAt = Instant.parse("2026-11-16T14:00:00Z")
        )
        coEvery { repository.getVouchers(any(), any(), any(), any()) } returns
            Result.success(listOf(vAcceptedWithBalance, vVoided, vCreditNote))

        val vm = createViewModel()
        settle()

        assertTrue(vm.uiState.value.hasLoaded)
        assertEquals(listOf("v3", "v2", "v1"), vm.uiState.value.filteredVouchers.map { it.id })

        vm.onFilterChange(VoucherFilter.ACCEPTED)
        assertEquals(listOf("v3", "v1"), vm.uiState.value.filteredVouchers.map { it.id })

        vm.onFilterChange(VoucherFilter.WITH_BALANCE)
        assertEquals(listOf("v1"), vm.uiState.value.filteredVouchers.map { it.id })

        vm.onFilterChange(VoucherFilter.VOIDED)
        assertEquals(listOf("v2"), vm.uiState.value.filteredVouchers.map { it.id })

        vm.onFilterChange(VoucherFilter.CREDIT_NOTES)
        assertEquals(listOf("v3"), vm.uiState.value.filteredVouchers.map { it.id })
    }

    @Test
    fun `sin permiso INVOICES_READ no llama al backend`() = runTest(mainRule.dispatcher) {
        val vm = createViewModel(listOf(Permission.PARTS_READ))
        settle()

        assertFalse(vm.uiState.value.canRead)
        assertTrue(vm.uiState.value.hasLoaded)
        coVerify(exactly = 0) { repository.getVouchers(any(), any(), any(), any()) }
    }

    @Test
    fun `error del backend muestra su message y al volver la red reintenta`() = runTest(mainRule.dispatcher) {
        online.value = false
        coEvery { repository.getVouchers(any(), any(), any(), any()) } returnsMany listOf(
            Result.failure(IOException()),
            Result.success(listOf(voucher("v1")))
        )
        val vm = createViewModel()
        settle()
        assertNotNull(vm.uiState.value.error)

        online.value = true
        settle()

        assertNull(vm.uiState.value.error)
        assertEquals(listOf("v1"), vm.uiState.value.vouchers.map { it.id })
    }

    @Test
    fun `anular comprobante con exito cierra la hoja, muestra aviso y recarga`() = runTest(mainRule.dispatcher) {
        val v = voucher("v1")
        coEvery { repository.getVouchers(any(), any(), any(), any()) } returnsMany listOf(
            Result.success(listOf(v)),
            Result.success(listOf(v.copy(status = VoucherStatus.VOIDED)))
        )
        coEvery { repository.voidVoucher("v1", "Error en RUC") } returns
            Result.success(v.copy(status = VoucherStatus.VOIDED))

        val vm = createViewModel()
        settle()

        vm.onOpenVoucher(v)
        assertTrue(vm.uiState.value.sheet is VoucherSheet.Detail)

        vm.onStartVoid()
        assertTrue(vm.uiState.value.sheet is VoucherSheet.Void)

        vm.onVoidReason("Error en RUC")
        vm.onConfirmVoid()
        settle()

        assertNull(vm.uiState.value.sheet)
        assertNotNull(vm.uiState.value.notice)
        assertEquals(VoucherStatus.VOIDED, vm.uiState.value.vouchers.single().status)
    }

    @Test
    fun `error al emitir nota de credito mantiene la hoja abierta con el message del backend`() = runTest(mainRule.dispatcher) {
        val v = voucher("v1", total = "200.00")
        coEvery { repository.getVouchers(any(), any(), any(), any()) } returns Result.success(listOf(v))
        coEvery { repository.getVoucher("v1") } returns Result.success(v)
        coEvery { repository.getBranchSeries() } returns Result.success(
            listOf(FiscalSeries("s1", "b1", VoucherType.NOTA_CREDITO, "FC01", 0, active = true))
        )
        coEvery { repository.issueCreditNote(v, "FC01", "Descuento", BigDecimal("50.00")) } returns
            Result.failure(ApiException(400, "BAD_REQUEST", "Serie no autorizada"))

        val vm = createViewModel()
        settle()

        vm.onOpenVoucher(v)
        vm.onStartCreditNote()
        val sheet = vm.uiState.value.sheet as VoucherSheet.CreditNote
        assertEquals("200.00", sheet.form.amount)

        vm.onCreditNoteReason("Descuento")
        vm.onCreditNoteAmount("50.00")
        vm.onConfirmCreditNote()
        settle()

        assertTrue(vm.uiState.value.sheet is VoucherSheet.CreditNote)
        assertEquals("Serie no autorizada", (vm.uiState.value.sheetError as UiText.Dynamic).value)
    }

    @Test
    fun `sin permiso INVOICES_ISSUE_SUNAT no abre hojas de anulacion ni nota de credito`() = runTest(mainRule.dispatcher) {
        val v = voucher("v1")
        coEvery { repository.getVouchers(any(), any(), any(), any()) } returns Result.success(listOf(v))

        val vm = createViewModel(listOf(Permission.INVOICES_READ))
        settle()

        vm.onOpenVoucher(v)
        vm.onStartVoid()
        assertTrue(vm.uiState.value.sheet is VoucherSheet.Detail)

        vm.onStartCreditNote()
        assertTrue(vm.uiState.value.sheet is VoucherSheet.Detail)
    }
}
