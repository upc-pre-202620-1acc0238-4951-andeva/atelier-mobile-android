package com.andeva.atelier.mobile.features.cashier.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.apiCallUnit
import com.andeva.atelier.mobile.core.data.remote.tenant.TenantService
import com.andeva.atelier.mobile.core.data.remote.tenant.UpdateTenantProfileRequestDto
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
import com.andeva.atelier.mobile.features.cashier.domain.TenantFiscalProfile
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
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
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.IssueCreditNoteRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.VoidVoucherRequestDto
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.VoucherLineRequestDto
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.saveHeaderOrDeleteClosed
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toDomain as toWorkOrder
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity as toWorkOrderEntity
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
import java.math.RoundingMode
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
    private val clock: Clock,
    private val tenantService: TenantService? = null
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

    override suspend fun getVouchers(
        type: VoucherType?,
        status: VoucherStatus?,
        from: LocalDate?,
        to: LocalDate?
    ): Result<List<Voucher>> {
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        return apiCall(gson, {
            cashierService.getVouchers(
                branchId = branchId,
                voucherType = type?.code,
                status = status?.name,
                from = from?.toString(),
                to = to?.toString()
            )
        }) { it }.mapCatching { dtos ->
            val vouchers = dtos.map { it.toDomain() }
            dtos.forEach { cacheHeader(it, it.workOrderId) }
            vouchers
        }
    }

    override suspend fun voidVoucher(voucherId: String, reason: String): Result<Voucher> {
        val trimmed = reason.trim()
        if (!CashierValidation.isValidReason(trimmed)) {
            return Result.failure(InvalidCashierInputException("reason"))
        }
        return apiCall(gson, { cashierService.voidVoucher(voucherId, VoidVoucherRequestDto(trimmed)) }) { it }
            .mapCatching { dto ->
                val voucher = dto.toDomain()
                cacheHeader(dto, dto.workOrderId)
                voucher
            }
    }

    override suspend fun issueCreditNote(
        referenceVoucher: Voucher,
        serie: String,
        reason: String,
        amount: BigDecimal
    ): Result<Voucher> {
        val trimmedReason = reason.trim()
        if (!CashierValidation.isValidReason(trimmedReason)) {
            return Result.failure(InvalidCashierInputException("reason"))
        }
        if (!CashierValidation.isValidMoneyAmount(amount, referenceVoucher.totalAmount.amount)) {
            return Result.failure(InvalidCashierInputException("amount"))
        }
        val trimmedSerie = serie.trim().uppercase()
        if (!CashierValidation.isCreditNoteSeries(trimmedSerie, referenceVoucher.type)) {
            return Result.failure(InvalidCashierInputException("serie"))
        }
        if (!referenceVoucher.isActiveSale || referenceVoucher.status == VoucherStatus.DRAFT) {
            return Result.failure(InvalidCashierInputException("status"))
        }
        val customerId = referenceVoucher.customerId
            ?: return Result.failure(InvalidCashierInputException("customerId"))
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }

        val normalizedAmount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        val fullMatch = normalizedAmount.compareTo(referenceVoucher.totalAmount.amount) == 0 &&
            referenceVoucher.lines.isNotEmpty() &&
            referenceVoucher.lines.all {
                it.description.isNotBlank() &&
                    it.unitPrice != null &&
                    it.unitPrice >= MIN_PAYMENT &&
                    it.quantity >= MIN_PAYMENT
            }
        val lines = if (fullMatch) {
            referenceVoucher.lines.map { line ->
                VoucherLineRequestDto(
                    description = line.description.take(CashierValidation.MAX_LINE_DESCRIPTION_LENGTH),
                    itemType = (line.itemType ?: VoucherItemType.SERVICE).name,
                    quantity = line.quantity,
                    unitPriceWithIgv = line.unitPrice!!,
                    itemId = line.itemId
                )
            }
        } else {
            listOf(
                VoucherLineRequestDto(
                    description = trimmedReason.take(CashierValidation.MAX_LINE_DESCRIPTION_LENGTH),
                    itemType = VoucherItemType.SERVICE.name,
                    quantity = BigDecimal.ONE,
                    unitPriceWithIgv = normalizedAmount,
                    itemId = null
                )
            )
        }
        val request = IssueCreditNoteRequestDto(
            branchId = branchId,
            customerId = customerId,
            lines = lines,
            reason = trimmedReason,
            reasonDescription = trimmedReason,
            referenceVoucherId = referenceVoucher.id,
            serie = trimmedSerie
        )
        return apiCall(gson, { cashierService.issueCreditNote(request) }) { it }.mapCatching { dto ->
            val voucher = dto.toDomain()
            cacheHeader(dto, dto.workOrderId)
            voucher
        }
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
     * ([IOException], respuesta ilegible o 2xx sin cuerpo) no se sabe si el backend lo creó: se relee la orden y, si hay
     * un comprobante vigente, es el creado; si no (o si la relectura también falla), se devuelve el error original. Un
     * error HTTP 4xx o 5xx (p. ej. 502 de SUNAT) se devuelve tal cual y no guarda nada. El envío no se reintenta solo. Guardar la cabecera en la caché es de mejor esfuerzo: nunca
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
        if (failure is ApiException && !failure.isSuccessStatus) {
            return Result.failure(failure) // error HTTP, también 5xx: el backend rechazó la emisión
        }
        if (failure != null) {
            // Sin respuesta, cuerpo ilegible o 2xx sin cuerpo: no se sabe si lo creó. Se relee la orden.
            val created = fetchWorkOrderVouchers(draft.workOrderId).getOrNull()?.firstOrNull { it.isActiveSale() }
                ?: return Result.failure(failure)
            return Result.success(issuedOutcome(created, draft.workOrderId))
        }
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
        return readVoucher(dto.id).getOrNull()?.let { IssueOutcome.Issued(it) } ?: IssueOutcome.IssuedPendingDetails(dto.id)
    }

    private suspend fun fetchWorkOrderVouchers(workOrderId: String) =
        guarded { cashierService.getWorkOrderVouchers(workOrderId) }

    /** [getVoucher] que nunca lanza: una respuesta ilegible también llega como fallo. */
    private suspend fun readVoucher(id: String): Result<Voucher> = try {
        getVoucher(id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

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
        val voucher = readVoucher(voucherId).getOrElse { return Result.failure(it) }
        // Anulado, rechazado por SUNAT o nota de crédito: no se cobra. Un estado desconocido o nulo cuenta como vigente.
        if (!voucher.isActiveSale) return Result.failure(InvalidCashierInputException("status"))
        if (amount > voucher.pendingBalance.amount) return Result.failure(InvalidCashierInputException("amount"))

        val currency = voucher.currency?.takeIf { it == Money.PEN || it == Money.USD }
            ?: return Result.failure(InvalidCashierInputException("currency"))
        val request = paymentRequest(voucher, currency, branchId, amount, method, reference)
        val payment = guarded { cashierService.registerPayment(request) }
        val failure = payment.exceptionOrNull()
        if (failure != null) {
            // Un 4xx es un rechazo: se muestra su message.
            if (failure is ApiException && !failure.isSuccessStatus && failure.httpStatus < SERVER_ERROR) {
                return Result.failure(failure)
            }
            // Sin respuesta, cuerpo ilegible, 2xx sin cuerpo o 5xx: no se sabe si el backend lo registró. Nunca se
            // reintenta; se relee (para la caché) y se avisa.
            readVoucher(voucherId)
            return Result.failure(PaymentOutcomeUnknownException(failure))
        }
        // Cobrado en el backend: si la relectura falla no se informa un error de cobro, sino que se revise el historial.
        return readVoucher(voucherId).recoverCatching { throw PaymentOutcomeUnknownException(it) }
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

    override suspend fun downloadVoucherDocument(url: String, fileNamePrefix: String): Result<File> {
        val ext = fileNamePrefix.substringAfterLast('.', "pdf").ifBlank { "pdf" }
        return pdfDownloader.downloadFromUrl(url = url, fallbackName = fileNamePrefix, extension = ext)
    }

    override suspend fun getTenantFiscalProfile(): Result<TenantFiscalProfile> {
        val service = tenantService ?: return Result.failure(IllegalStateException("Servicio de taller no disponible"))
        return apiCall(gson, { service.getCurrent() }) { dto ->
            TenantFiscalProfile(
                id = dto.id,
                name = dto.name.orEmpty(),
                legalName = dto.legalName.orEmpty(),
                taxId = dto.taxId.orEmpty(),
                status = dto.status
            )
        }.onSuccess { profile ->
            if (profile.name.isNotBlank()) {
                sessionManager.updateTenantName(profile.name)
            }
        }
    }

    override suspend fun updateTenantFiscalProfile(
        name: String,
        legalName: String
    ): Result<TenantFiscalProfile> {
        val trimmedName = name.trim()
        val trimmedLegalName = legalName.trim()
        if (trimmedName.length !in 1..100) return Result.failure(InvalidCashierInputException("tenantName"))
        if (trimmedLegalName.length !in 1..150) return Result.failure(InvalidCashierInputException("tenantLegalName"))
        val service = tenantService ?: return Result.failure(IllegalStateException("Servicio de taller no disponible"))
        val body = UpdateTenantProfileRequestDto(
            name = trimmedName,
            legalName = trimmedLegalName
        )
        return apiCall(gson, { service.updateCurrent(body) }) { dto ->
            TenantFiscalProfile(
                id = dto.id,
                name = dto.name.orEmpty(),
                legalName = dto.legalName.orEmpty(),
                taxId = dto.taxId.orEmpty(),
                status = dto.status
            )
        }.onSuccess { profile ->
            if (profile.name.isNotBlank()) {
                sessionManager.updateTenantName(profile.name)
            }
        }
    }

    /** Mejor esfuerzo, como [cacheHeader]: si Room falla se devuelve igual la orden que respondió el backend. */
    private suspend fun saveWorkOrderHeader(dto: WorkOrderDto): WorkOrder {
        val now = clock.millis()
        val fromBackend = dto.toWorkOrderEntity(now).toWorkOrder()
        return try {
            workOrderDao.saveHeaderOrDeleteClosed(dto, now)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fromBackend
        }
    }

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
        const val SERIES_LENGTH = 4
        const val SERVER_ERROR = 500

        /** Un 2xx que llega como error: `apiCall` lo da así si no hay cuerpo (`EMPTY_BODY`). */
        val ApiException.isSuccessStatus: Boolean get() = httpStatus in 200..299
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
