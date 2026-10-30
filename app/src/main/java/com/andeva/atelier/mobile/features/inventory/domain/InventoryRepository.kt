package com.andeva.atelier.mobile.features.inventory.domain

import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.Instant

/**
 * Inventario (US31, US32, US34). Lecturas offline-first con las cachés `parts` y `batches`
 * (docs/06-offline-sync.md §4); recibir una compra es en línea (06 §2): sin red falla con `IOException` y no se encola.
 */
interface InventoryRepository {
    /** Recarga el catálogo completo (página por página); con [force] ignora la vigencia de 1 h. */
    suspend fun refreshCatalog(force: Boolean = false): Result<Unit>

    fun observeCatalog(category: ItemCategory?, brand: String? = null): Flow<List<Part>>

    /** Marcas distintas del catálogo guardado, en orden alfabético. Vacío mientras el backend no envíe la marca (H35). */
    fun observeBrands(): Flow<List<String>>

    /** Por nombre o SKU, con categoría y marca opcionales. Sin red busca en la caché. */
    suspend fun searchParts(query: String, category: ItemCategory?, brand: String? = null): Result<List<Part>>

    /** null = no existe (404). Sin red busca en la caché. */
    suspend fun findBySku(sku: String): Result<Part?>

    fun observePart(id: String): Flow<Part?>

    /** Repuesto con sus lotes en orden FIFO (lotes solo con `inventory:batches:read`). Sin red, desde la caché. */
    suspend fun getPartDetail(id: String): Result<PartDetail>

    /** Lotes del repuesto (1 h; con [force] ignora la vigencia). Sin `inventory:batches:read` no hace nada. */
    suspend fun refreshBatches(itemId: String, force: Boolean = false): Result<Unit>

    fun observeBatches(itemId: String): Flow<List<Batch>>

    /** Bandeja de alertas en línea; sin red se deriva de la caché `parts`. */
    suspend fun getStockAlerts(): Result<List<StockAlert>>

    /** Repuestos en el mínimo o debajo según la caché: se actualiza solo al recargar el catálogo. */
    fun observeLowStock(): Flow<List<Part>>

    suspend fun getValuation(): Result<InventoryValuation>

    suspend fun getPurchaseOrders(status: PurchaseOrderStatus?): Result<List<PurchaseOrder>>

    suspend fun getPurchaseOrder(id: String): Result<PurchaseOrder>

    /**
     * Sube el comprobante y recibe la orden; luego recarga el catálogo y los lotes recibidos. Si la orden ya estaba
     * `RECEIVED` responde éxito sin subir nada (H15).
     */
    suspend fun receivePurchaseOrder(id: String, receiptNumber: String, photo: File, receivedAt: Instant?): Result<PurchaseOrder>
}
