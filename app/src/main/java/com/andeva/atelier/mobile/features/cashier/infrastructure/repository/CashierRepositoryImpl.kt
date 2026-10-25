package com.andeva.atelier.mobile.features.cashier.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.apiCallUnit
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.DailyCashSummary
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.Payment
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.PaymentOutcomeUnknownException
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherSummary
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherDao
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.isActiveSale
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.paymentRequest
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.toRequest
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.CashierService
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.ConfigureSeriesRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.UpdateSeriesStatusRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.ElectronicVoucherDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.HandoverService
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.saveHeaderOrDeleteClosed
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprobantes de caja (docs/api/invoicing.md). La caché `vouchers` guarda solo cabeceras; el detalle completo siempre
 * sale de la red. Emitir nunca duplica un comprobante fiscal: ver [issue]. Es `@Singleton` porque guarda los candados
 * que serializan la emisión por orden y el cobro por comprobante dentro del proceso (doble toque).
 */
@Singleton
class CashierRepositoryImpl @Inject constructor(
    private val cashierService: CashierService,
    private val handoverService: HandoverService,
    private val voucherDao: VoucherDao,
    private val workOrderDao: WorkOrderDao,
    private val pdfDownloader: PdfDownloader,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : CashierRepository {

    private val issueLocks = KeyedMutex()
    private val paymentLocks = KeyedMutex()

    override fun observeWorkOrderVouchers(workOrderId: String): Flow<List<VoucherSummary>> =
        voucherDao.observeByWorkOrder(workOrderId).map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun refreshWorkOrderVouchers(workOrderId: String): Result<List<Voucher>> =
        fetchWorkOrderVouchers(workOrderId).mapCatching { dtos ->
            val now = clock.millis()
            voucherDao.replaceWorkOrder(workOrderId, dtos.mapNotNull { it.toEntity(workOrderId, now) })
            dtos.map { it.toDomain() }
        }

    override suspend fun getVoucher(id: String): Result<Voucher> =
        apiCall(gson, { cashierService.getVoucher(id) }) { it }.mapCatching { dto ->
            cacheHeader(dto, fallbackWorkOrderId = null)
            dto.toDomain()
        }

    override suspend fun getBranchSeries(): Result<List<FiscalSeries>> {
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        return apiCall(gson, { cashierService.getBranchSeries(branchId) }) { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun configureSeries(
        type: VoucherType,
        serie: String,
        initialCorrelative: Int?
    ): Result<FiscalSeries> {
        if (!CashierValidation.isSeriesFor(type, serie)) return Result.failure(InvalidCashierInputException("serie"))
        if (initialCorrelative != null && initialCorrelative < 0) {
            return Result.failure(InvalidCashierInputException("initialCorrelative"))
        }
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        val request = ConfigureSeriesRequestDto(branchId, serie, type.code, initialCorrelative)
        return apiCall(gson, { cashierService.configureSeries(request) }) { it.toDomain() }
    }

    override suspend fun setSeriesActive(id: String, active: Boolean): Result<Unit> =
        apiCallUnit(gson) { cashierService.updateSeriesStatus(id, UpdateSeriesStatusRequestDto(active)) }

    /**
     * Antes de emitir se lee la orden: si ya tiene una factura o boleta vigente se devuelve esa y no se envía nada
     * (doble toque, otro cajero). Si no se puede verificar, no se emite. Si el envío termina sin respuesta
     * ([IOException] o respuesta ilegible) no se sabe si el backend lo creó: se relee la orden y, si hay un comprobante
     * vigente, es el creado; si no, se devuelve el error original. Un error HTTP (p. ej. 502 de SUNAT) se devuelve tal
     * cual y no guarda nada. El envío no se reintenta solo. Guardar la cabecera en la caché es de mejor esfuerzo: nunca
     * cambia el resultado que ya dio el backend. Dos emisiones de la misma orden en este proceso van una tras otra: la
     * segunda ya ve el comprobante de la primera.
     */
    override suspend fun issue(draft: VoucherDraft, serie: String): Result<IssueOutcome> {
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        return issueLocks.withLock(draft.workOrderId) { issueOnce(draft, serie, branchId) }
    }

    private suspend fun issueOnce(draft: VoucherDraft, serie: String, branchId: String): Result<IssueOutcome> {
        val existing = fetchWorkOrderVouchers(draft.workOrderId).getOrElse { return Result.failure(it) }
            .firstOrNull { it.isActiveSale() }
        if (existing != null) {
            cacheHeader(existing, draft.workOrderId)
            return runCatching { IssueOutcome.AlreadyIssued(existing.toDomain()) }
        }

        val issued = guarded { cashierService.issueVoucher(draft.toRequest(branchId, serie)) }
        val failure = issued.exceptionOrNull()
        if (failure != null && failure !is ApiException) {
            val created = fetchWorkOrderVouchers(draft.workOrderId).getOrNull()?.firstOrNull { it.isActiveSale() }
                ?: return Result.failure(failure)
            return Result.success(issuedOutcome(created, draft.workOrderId))
        }
        if (failure != null) return Result.failure(failure) // error HTTP: el backend rechazó la emisión
        // El backend ya lo emitió: desde aquí nunca se devuelve un error.
        return Result.success(issuedOutcome(issued.getOrThrow(), draft.workOrderId))
    }

    /**
     * Resultado de un comprobante que el backend ya emitió. Si su respuesta no se puede mapear (falta un monto) se
     * relee por id; si tampoco, el comprobante existe pero sin detalle ([IssueOutcome.IssuedPendingDetails]).
     */
    private suspend fun issuedOutcome(dto: ElectronicVoucherDto, workOrderId: String): IssueOutcome {
        cacheHeader(dto, workOrderId)
        runCatching { dto.toDomain() }.getOrNull()?.let { return IssueOutcome.Issued(it) }
        val reread = try {
            getVoucher(dto.id).getOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return reread?.let { IssueOutcome.Issued(it) } ?: IssueOutcome.IssuedPendingDetails(dto.id)
    }

    private suspend fun fetchWorkOrderVouchers(workOrderId: String) =
        apiCall(gson, { cashierService.getWorkOrderVouchers(workOrderId) }) { it }

    /** Mejor esfuerzo: la cabecera en caché es de cortesía y su fallo nunca desmiente lo que dijo el backend. */
    private suspend fun cacheHeader(dto: ElectronicVoucherDto, fallbackWorkOrderId: String?) {
        try {
            dto.toEntity(fallbackWorkOrderId, clock.millis())?.let { voucherDao.upsert(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun sessionBranchId(): Result<String> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        return session.branchId?.let { Result.success(it) } ?: Result.failure(MissingBranchException())
    }

    override suspend fun registerPayment(
        voucherId: String,
        amount: BigDecimal,
        method: PaymentMethod,
        transactionReference: String?
    ): Result<Voucher> {
        val reference = transactionReference?.trim()?.ifEmpty { null }
        if (amount < MIN_PAYMENT || amount.stripTrailingZeros().scale() > MONEY_SCALE) {
            return Result.failure(InvalidCashierInputException("amount"))
        }
        if (reference != null && reference.length > MAX_REFERENCE_LENGTH) {
            return Result.failure(InvalidCashierInputException("transactionReference"))
        }
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        // Dos cobros del mismo comprobante en este proceso van uno tras otro: el segundo valida contra el saldo nuevo.
        return paymentLocks.withLock(voucherId) { payOnce(voucherId, amount, method, reference, branchId) }
    }

    private suspend fun payOnce(
        voucherId: String,
        amount: BigDecimal,
        method: PaymentMethod,
        reference: String?,
        branchId: String
    ): Result<Voucher> {
        // El saldo sale siempre del comprobante, leído ahora (riesgo 3), nunca de la orden.
        val voucher = getVoucher(voucherId).getOrElse { return Result.failure(it) }
        if (amount > voucher.pendingBalance.amount) return Result.failure(InvalidCashierInputException("amount"))

        val currency = voucher.currency?.takeIf { it == Money.PEN || it == Money.USD }
            ?: return Result.failure(InvalidCashierInputException("currency"))
        val request = paymentRequest(voucher, currency, branchId, amount, method, reference)
        val payment = guarded { cashierService.registerPayment(request) }
        val failure = payment.exceptionOrNull()
        if (failure != null) {
            if (failure is ApiException) return Result.failure(failure)
            // Sin respuesta: no se sabe si el backend lo registró. Nunca se reintenta; se relee y se avisa.
            getVoucher(voucherId)
            return Result.failure(PaymentOutcomeUnknownException(failure))
        }
        // Cobrado en el backend: si la relectura falla no se informa un error de cobro, sino que se revise el historial.
        return getVoucher(voucherId).recoverCatching { throw PaymentOutcomeUnknownException(it) }
    }

    override suspend fun getPayments(voucherId: String): Result<List<Payment>> =
        apiCall(gson, { cashierService.getVoucherPayments(voucherId) }) { it }
            .mapCatching { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getDailyCashSummary(date: LocalDate): Result<DailyCashSummary> {
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        return apiCall(gson, { cashierService.getDailyReconciliation(branchId, date.toString()) }) { it }
            .mapCatching { it.toDomain(date) }
    }

    override suspend fun markWorkOrderPaid(workOrderId: String): Result<WorkOrder> =
        apiCall(gson, { handoverService.markAsPaid(workOrderId) }) { it }.mapCatching { saveWorkOrderHeader(it) }

    override suspend fun downloadExitPass(workOrderId: String): Result<File> =
        pdfDownloader.download("pase-salida-$workOrderId") { handoverService.getExitPassPdf(workOrderId) }

    override suspend fun deliver(workOrderId: String): Result<WorkOrder> =
        apiCall(gson, { handoverService.deliver(workOrderId) }) { it }.mapCatching { saveWorkOrderHeader(it) }

    private suspend fun saveWorkOrderHeader(dto: WorkOrderDto) = workOrderDao.saveHeaderOrDeleteClosed(dto, clock.millis())

    /** Como [apiCall], pero una respuesta ilegible (no solo un corte de red) también llega como fallo. */
    private suspend fun <T> guarded(call: suspend () -> Response<T>): Result<T> = try {
        apiCall(gson, call) { it }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        val MIN_PAYMENT = BigDecimal("0.01")
        const val MONEY_SCALE = 2
        const val MAX_REFERENCE_LENGTH = 100
    }
}

/** Un [Mutex] por clave (orden o comprobante), no uno global; la entrada se libera cuando nadie la usa. */
private class KeyedMutex {
    private class Entry {
        val mutex = Mutex()
        var users = 0
    }

    private val entries = HashMap<String, Entry>()

    suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val entry = synchronized(entries) { entries.getOrPut(key) { Entry() }.also { it.users++ } }
        try {
            return entry.mutex.withLock { block() }
        } finally {
            synchronized(entries) { if (--entry.users == 0) entries.remove(key) }
        }
    }
}
