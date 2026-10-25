package com.andeva.atelier.mobile.features.cashier.domain

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/** Resultado de emitir: el comprobante nuevo, o el que la orden ya tenía vigente (no se emite otro). */
sealed interface IssueOutcome {
    val voucherId: String

    data class Issued(val voucher: Voucher) : IssueOutcome {
        override val voucherId: String get() = voucher.id
    }

    data class AlreadyIssued(val voucher: Voucher) : IssueOutcome {
        override val voucherId: String get() = voucher.id
    }

    /** El comprobante ya existe en el backend, pero su respuesta no se pudo leer completa: la UI debe recargarlo. */
    data class IssuedPendingDetails(override val voucherId: String) : IssueOutcome
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
     * Registra una serie en la sede de la sesión (`voucherType` viaja como código SUNAT). No llama al backend si la
     * serie no corresponde al tipo (`InvalidCashierInputException("serie")`) o si [initialCorrelative] es negativo
     * (`InvalidCashierInputException("initialCorrelative")`). Una serie repetida da el `message` del 409.
     */
    suspend fun configureSeries(type: VoucherType, serie: String, initialCorrelative: Int?): Result<FiscalSeries>

    /** Activa o desactiva una serie. */
    suspend fun setSeriesActive(id: String, active: Boolean): Result<Unit>

    /**
     * Emite el comprobante con la sede de la sesión. Nunca duplica: si la orden ya tiene una factura o boleta vigente
     * la devuelve como [IssueOutcome.AlreadyIssued] sin emitir, y tras un corte de red o un 2xx sin cuerpo legible
     * relee la orden antes de rendirse (el backend no documenta un reintento seguro). Dos emisiones de la misma orden en
     * el proceso van una tras otra.
     */
    suspend fun issue(draft: VoucherDraft, serie: String): Result<IssueOutcome>

    /**
     * Registra un cobro. Antes de enviar lee el comprobante en la red (el saldo sale de su `pendingBalance`, nunca de
     * la orden) y no llama al backend si [amount] es menor a 0.01, tiene más de 2 decimales o supera el saldo
     * (`InvalidCashierInputException("amount")`), o si [transactionReference] pasa de 100 caracteres
     * (`InvalidCashierInputException("transactionReference")`). La moneda es la del comprobante y la sede, la de la
     * sesión. Devuelve el comprobante releído. Si el envío termina sin respuesta, con un 5xx o con un 2xx sin cuerpo
     * legible no se reintenta: se relee el comprobante y falla con [PaymentOutcomeUnknownException], para revisar el
     * historial antes de volver a cobrar. Un 4xx falla con el `message` del backend. Dos cobros del mismo comprobante en
     * el proceso van uno tras otro.
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
