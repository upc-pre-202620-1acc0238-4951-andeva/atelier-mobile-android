package com.andeva.atelier.mobile.features.cashier.domain

import kotlinx.coroutines.flow.Flow

/** Resultado de emitir: el comprobante nuevo, o el que la orden ya tenía vigente (no se emite otro). */
sealed interface IssueOutcome {
    val voucher: Voucher

    data class Issued(override val voucher: Voucher) : IssueOutcome

    data class AlreadyIssued(override val voucher: Voucher) : IssueOutcome
}

/** Contrato de caja; los cobros y la entrega llegan en los pasos B4 y B5 del paso 9. */
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
}
