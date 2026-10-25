package com.andeva.atelier.mobile.features.cashier.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.IssueOutcome
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherSummary
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherDao
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.isActiveSale
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.toRequest
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.CashierService
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.ElectronicVoucherDto
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/**
 * Comprobantes de caja (docs/api/invoicing.md). La caché `vouchers` guarda solo cabeceras; el detalle completo siempre
 * sale de la red. Emitir nunca duplica un comprobante fiscal: ver [issue].
 */
class CashierRepositoryImpl @Inject constructor(
    private val cashierService: CashierService,
    private val voucherDao: VoucherDao,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : CashierRepository {

    override fun observeWorkOrderVouchers(workOrderId: String): Flow<List<VoucherSummary>> =
        voucherDao.observeByWorkOrder(workOrderId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshWorkOrderVouchers(workOrderId: String): Result<List<Voucher>> =
        fetchWorkOrderVouchers(workOrderId).mapCatching { dtos ->
            val now = clock.millis()
            voucherDao.replaceWorkOrder(workOrderId, dtos.map { it.toEntity(workOrderId, now) })
            dtos.map { it.toDomain() }
        }

    override suspend fun getVoucher(id: String): Result<Voucher> =
        apiCall(gson, { cashierService.getVoucher(id) }) { it }.mapCatching { dto ->
            voucherDao.upsert(dto.toEntity(fallbackWorkOrderId = null, syncedAt = clock.millis()))
            dto.toDomain()
        }

    override suspend fun getBranchSeries(): Result<List<FiscalSeries>> {
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }
        return apiCall(gson, { cashierService.getBranchSeries(branchId) }) { dtos -> dtos.map { it.toDomain() } }
    }

    /**
     * Antes de emitir se lee la orden: si ya tiene una factura o boleta vigente se devuelve esa y no se envía nada
     * (doble toque, otro cajero). Si no se puede verificar, no se emite. Si el envío termina sin respuesta
     * ([IOException] o respuesta ilegible) no se sabe si el backend lo creó: se relee la orden y, si hay un comprobante vigente, es el
     * creado; si no, se devuelve el error original. Guardar la cabecera en la caché es de mejor esfuerzo. Un error HTTP (p. ej. 502 de SUNAT) se devuelve tal cual y no
     * guarda nada. El envío no se reintenta solo.
     */
    override suspend fun issue(draft: VoucherDraft, serie: String): Result<IssueOutcome> {
        val branchId = sessionBranchId().getOrElse { return Result.failure(it) }

        val existing = fetchWorkOrderVouchers(draft.workOrderId).getOrElse { return Result.failure(it) }
            .firstOrNull { it.isActiveSale() }
        if (existing != null) {
            cacheHeader(existing, draft.workOrderId)
            return runCatching { IssueOutcome.AlreadyIssued(existing.toDomain()) }
        }

        val issued = try {
            apiCall(gson, { cashierService.issueVoucher(draft.toRequest(branchId, serie)) }) { it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e) // respuesta ilegible: el backend pudo haberlo creado, igual que un corte de red
        }
        val failure = issued.exceptionOrNull()
        if (failure != null && failure !is ApiException) {
            val created = fetchWorkOrderVouchers(draft.workOrderId).getOrNull()?.firstOrNull { it.isActiveSale() }
                ?: return Result.failure(failure)
            cacheHeader(created, draft.workOrderId)
            return runCatching { IssueOutcome.Issued(created.toDomain()) }
        }
        // El backend ya lo emitió: un fallo al guardar la caché no puede convertir el resultado en error.
        return issued.mapCatching { dto ->
            cacheHeader(dto, draft.workOrderId)
            IssueOutcome.Issued(dto.toDomain())
        }
    }

    private suspend fun fetchWorkOrderVouchers(workOrderId: String) =
        apiCall(gson, { cashierService.getWorkOrderVouchers(workOrderId) }) { it }

    /** Mejor esfuerzo: la cabecera en caché es de cortesía y su fallo nunca desmiente lo que dijo el backend. */
    private suspend fun cacheHeader(dto: ElectronicVoucherDto, fallbackWorkOrderId: String?) {
        try {
            voucherDao.upsert(dto.toEntity(fallbackWorkOrderId, clock.millis()))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun sessionBranchId(): Result<String> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        return session.branchId?.let { Result.success(it) } ?: Result.failure(MissingBranchException())
    }
}
