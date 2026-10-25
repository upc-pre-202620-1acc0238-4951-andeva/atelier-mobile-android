package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/** Resultado de emitir: el comprobante nuevo, o el que la orden ya tenía vigente (no se emite otro). */
sealed interface IssueOutcome {
    val voucher: Voucher

    data class Issued(override val voucher: Voucher) : IssueOutcome

    data class AlreadyIssued(override val voucher: Voucher) : IssueOutcome
}

/** Contrato de caja: comprobantes, cobros, caja del día, pase de salida y entrega. */
interface CashierRepository {
    /** Cabeceras en caché de los comprobantes de la orden (caché `vouchers`, "por orden consultada"). */
    fun observeWorkOrderVouchers(workOrderId: String): Flow<List<VoucherSummary>>

    /** Lee los comprobantes de la orden en la red, reemplaza sus cabeceras en la caché y devuelve los completos. */
    suspend fun refreshWorkOrderVouchers(workOrderId: String): Result<List<Voucher>>

    /** Lee un comprobante en la red y deja su cabecera en la caché. */
    suspend fun getVoucher(id: String): Result<Voucher>

    /** Series de la sede de la sesión; falla con `MissingBranchException` si la sesión no tiene sede. */
    suspend fun getBranchSeries(): Result<List<FiscalSeries>>

    /**
     * Emite el comprobante con la sede de la sesión. Nunca duplica: si la orden ya tiene una factura o boleta vigente
     * la devuelve como [IssueOutcome.AlreadyIssued] sin emitir, y tras un corte de red relee la orden antes de
     * rendirse (el backend no documenta un reintento seguro).
     */
    suspend fun issue(draft: VoucherDraft, serie: String): Result<IssueOutcome>

    /**
     * Registra un cobro. Antes de enviar lee el comprobante en la red (el saldo sale de su `pendingBalance`, nunca de
     * la orden) y no llama al backend si [amount] es menor a 0.01, tiene más de 2 decimales o supera el saldo
     * (`InvalidCashierInputException("amount")`), o si [transactionReference] pasa de 100 caracteres
     * (`InvalidCashierInputException("transactionReference")`). La moneda es la del comprobante y la sede, la de la
     * sesión. Devuelve el comprobante releído. Si el envío termina sin respuesta no se reintenta: se relee el
     * comprobante y falla con [PaymentOutcomeUnknownException], para revisar el historial antes de volver a cobrar.
     */
    suspend fun registerPayment(
        voucherId: String,
        amount: BigDecimal,
        method: PaymentMethod,
        transactionReference: String?
    ): Result<Voucher>

    /** Historial de cobros del comprobante, leído en la red. */
    suspend fun getPayments(voucherId: String): Result<List<Payment>>

    /** Caja de la sede de la sesión en [date]; falla con `MissingBranchException` sin llamar si la sesión no tiene sede. */
    suspend fun getDailyCashSummary(date: LocalDate): Result<DailyCashSummary>

    /** Pago formal de la orden (`mark-as-paid`). Guarda la cabecera; una orden cerrada sale de la caché de la sede. */
    suspend fun markWorkOrderPaid(workOrderId: String): Result<WorkOrder>

    /** Descarga el pase de salida en PDF; una orden sin pagar da el `message` del 422 del backend. */
    suspend fun downloadExitPass(workOrderId: String): Result<File>

    /** Entrega el vehículo. Guarda la cabecera; una orden cerrada sale de la caché de la sede. */
    suspend fun deliver(workOrderId: String): Result<WorkOrder>
}
