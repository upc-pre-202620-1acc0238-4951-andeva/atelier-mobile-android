package com.andeva.atelier.mobile.features.inventory.infrastructure.repository

import android.content.Context
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.media.StoragePaths
import com.andeva.atelier.mobile.core.media.SupabaseUploader
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValuation
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartDetail
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.domain.StockAlert
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.BatchDao
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.BatchEntity
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.PartDao
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.InventoryService
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.PartDto
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.ReceivePurchaseOrderRequestDto
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Offline-first para leer (docs/06-offline-sync.md §4): el catálogo y los lotes se guardan en `parts` y `batches` y la
 * UI observa Room. Recibir una compra es en línea (06 §2): la foto del comprobante se sube directo, sin la cola.
 */
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryService: InventoryService,
    private val partDao: PartDao,
    private val batchDao: BatchDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val uploader: SupabaseUploader,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val clock: Clock
) : InventoryRepository {

    /**
     * El backend pagina en memoria y no da el total: se piden páginas hasta recibir menos de [PAGE_SIZE] (con un tope por
     * si no corta). Solo se reemplaza la caché cuando llegaron todas; si una falla, se conserva la anterior.
     */
    override suspend fun refreshCatalog(force: Boolean): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (!session.has(Permission.PARTS_READ)) return Result.success(Unit)
        val now = clock.millis()
        if (!force && !isStale(InventoryCachePolicy.SCOPE_CATALOG, InventoryCachePolicy.CATALOG_TTL_MILLIS, now)) return Result.success(Unit)

        val all = mutableListOf<PartDto>()
        for (page in 0 until InventoryCachePolicy.MAX_PAGES) {
            val dtos = apiCall(gson, { inventoryService.getItems(page = page, size = InventoryCachePolicy.PAGE_SIZE) }) { it }
                .getOrElse { return Result.failure(it) }
            all += dtos
            if (dtos.size < InventoryCachePolicy.PAGE_SIZE) break
        }
        val rows = all.distinctBy { it.id }.map { it.toEntity(now) }
        partDao.replaceAll(rows)
        cacheMetadataDao.upsert(CacheMetadataEntity(InventoryCachePolicy.SCOPE_CATALOG, now, rows.size))
        return Result.success(Unit)
    }

    override fun observeCatalog(category: ItemCategory?): Flow<List<Part>> =
        partDao.observeAll(category?.name).map { rows -> rows.map { it.toDomain() } }

    override suspend fun searchParts(query: String, category: ItemCategory?): Result<List<Part>> {
        val text = query.trim()
        val result = apiCall(gson, { inventoryService.getItems(search = text.ifEmpty { null }, category = category?.name, size = SEARCH_SIZE) }) { it }
            .map { dtos -> savePartsFromServer(dtos) }
        if (result.exceptionOrNull() !is IOException) return result
        return Result.success(partDao.search(text, category?.name).map { it.toDomain() })
    }

    override suspend fun findBySku(sku: String): Result<Part?> {
        val normalized = InventoryValidation.normalizeSku(sku)
        val result = apiCall(gson, { inventoryService.getBySku(normalized) }) { it }
        val error = result.exceptionOrNull()
        return when {
            error == null -> Result.success(savePartsFromServer(listOf(result.getOrThrow())).single())
            error is ApiException && error.httpStatus == 404 -> Result.success(null)
            error is IOException -> Result.success(partDao.findBySku(normalized)?.toDomain())
            else -> Result.failure(error)
        }
    }

    override fun observePart(id: String): Flow<Part?> = partDao.observeById(id).map { it?.toDomain() }

    /** Los lotes en línea traen más campos que la caché; sin `batches:read` el detalle va sin lotes. */
    override suspend fun getPartDetail(id: String): Result<PartDetail> {
        val canReadBatches = sessionManager.getSession()?.has(Permission.BATCHES_READ) == true
        val result = apiCall(gson, { inventoryService.getItem(id) }) { it }
        val error = result.exceptionOrNull()
        if (error is IOException) {
            val part = partDao.findById(id)?.toDomain() ?: return Result.failure(error)
            val batches = if (canReadBatches) batchDao.findForItem(id).map { it.toDomain() } else emptyList()
            return Result.success(PartDetail(part, batches))
        }
        return result.map { dto ->
            val part = savePartsFromServer(listOf(dto.toPart())).single()
            if (!canReadBatches) return@map PartDetail(part, emptyList())
            val batches = dto.batches.orEmpty()
            saveBatches(id, batches.map { it.toEntity(id, clock.millis()) })
            PartDetail(part, batches.map { it.toDomain(id) }.sortedFifo())
        }
    }

    override suspend fun refreshBatches(itemId: String, force: Boolean): Result<Unit> {
        if (sessionManager.getSession()?.has(Permission.BATCHES_READ) != true) return Result.success(Unit)
        val now = clock.millis()
        val scope = InventoryCachePolicy.batchesScope(itemId)
        if (!force && !isStale(scope, InventoryCachePolicy.BATCHES_TTL_MILLIS, now)) return Result.success(Unit)
        return apiCall(gson, { inventoryService.getBatches(itemId) }) { it }
            .map { dtos -> saveBatches(itemId, dtos.map { it.toEntity(itemId, now) }) }
    }

    override fun observeBatches(itemId: String): Flow<List<Batch>> =
        batchDao.observeForItem(itemId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getStockAlerts(): Result<List<StockAlert>> {
        val result = apiCall(gson, { inventoryService.getAlerts() }) { dtos -> dtos.map { it.toDomain() } }
        if (result.exceptionOrNull() !is IOException) return result
        return Result.success(partDao.findLowStock().map { it.toDomain().toStockAlert() })
    }

    override fun observeLowStock(): Flow<List<Part>> = partDao.observeLowStock().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getValuation(): Result<InventoryValuation> =
        apiCall(gson, { inventoryService.getValuation() }) {
            InventoryValuation(Money(it.valuation ?: BigDecimal.ZERO, Money.PEN))
        }

    override suspend fun getPurchaseOrders(status: PurchaseOrderStatus?): Result<List<PurchaseOrder>> =
        apiCall(gson, { inventoryService.getPurchaseOrders(status?.name) }) { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getPurchaseOrder(id: String): Result<PurchaseOrder> =
        apiCall(gson, { inventoryService.getPurchaseOrder(id) }) { it.toDomain() }

    /**
     * H15 de docs/03-traceability.md: el backend responde 400 a todo y no es idempotente. Se lee la orden antes de subir
     * la foto (ya `RECEIVED` = éxito sin subir nada) y, si el `PUT` falla con 400 o se corta la red, se vuelve a leer.
     */
    override suspend fun receivePurchaseOrder(id: String, receiptNumber: String, photo: File, receivedAt: Instant?): Result<PurchaseOrder> {
        val current = getPurchaseOrder(id).getOrElse { return Result.failure(it) }
        if (current.status == PurchaseOrderStatus.RECEIVED) return Result.success(current)
        if (!current.canBeReceived) {
            return Result.failure(IllegalStateException("La orden de compra no está emitida (${current.status})"))
        }
        val tenantId = sessionManager.getSession()?.tenantId ?: return Result.failure(IllegalStateException("Sin sesión"))

        val photoId = StoragePaths.newId()
        val local = File(context.cacheDir, "receipts/$photoId.jpg")
        val upload = try {
            runCatching { uploader.compress(photo, local) }
                .mapCatching { uploader.upload(it, StoragePaths.purchaseReceipt(tenantId, id, photoId)).getOrThrow() }
        } finally {
            local.delete()
        }
        val imageUrl = upload.getOrElse { return Result.failure(it) }

        val body = ReceivePurchaseOrderRequestDto(imageUrl, receiptNumber.trim(), receivedAt?.toString())
        val result = apiCall(gson, { inventoryService.receivePurchaseOrder(id, body) }) { it.toDomain() }
        val error = result.exceptionOrNull()
        val received = when {
            error == null -> result.getOrThrow()
            error is IOException || (error is ApiException && error.httpStatus == 400) ->
                getPurchaseOrder(id).getOrNull()?.takeIf { it.status == PurchaseOrderStatus.RECEIVED } ?: return Result.failure(error)
            else -> return Result.failure(error)
        }
        afterReceive(received)
        return Result.success(received)
    }

    /** El stock subió: catálogo completo (la alerta sale sola de la bandeja, US34 E2) y lotes de lo recibido. */
    private suspend fun afterReceive(order: PurchaseOrder) {
        refreshCatalog(force = true)
        order.lines.map { it.itemId }.distinct().forEach { refreshBatches(it, force = true) }
    }

    private suspend fun savePartsFromServer(dtos: List<PartDto>): List<Part> {
        val rows = dtos.map { it.toEntity(clock.millis()) }
        partDao.upsertAll(rows)
        return rows.map { it.toDomain() }
    }

    private suspend fun saveBatches(itemId: String, rows: List<BatchEntity>) {
        val now = clock.millis()
        batchDao.replaceForItem(itemId, rows)
        cacheMetadataDao.upsert(CacheMetadataEntity(InventoryCachePolicy.batchesScope(itemId), now, rows.size))
    }

    private suspend fun isStale(scope: String, ttlMillis: Long, now: Long): Boolean {
        val lastSyncedAt = cacheMetadataDao.get(scope)?.lastSyncedAt ?: return true
        return now - lastSyncedAt >= ttlMillis
    }

    /** FIFO: el lote más antiguo primero (el backend los manda del más nuevo al más viejo). */
    private fun List<Batch>.sortedFifo() = sortedBy { it.arrivalDate ?: Instant.MAX }

    companion object {
        private const val SEARCH_SIZE = 50
    }
}
