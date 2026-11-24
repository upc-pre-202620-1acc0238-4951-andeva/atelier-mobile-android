package com.andeva.atelier.mobile.features.inventory.domain

import kotlinx.coroutines.flow.Flow
import java.io.File
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventario (US31–US35). Lecturas offline-first con las cachés `parts` y `batches`
 * (docs/06-offline-sync.md §4); mutaciones y compras son en línea (06 §2): sin red fallan con `IOException` y no se encolan.
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

    /** Crea un repuesto nuevo (`inventory:parts:manage`) y lo guarda en Room. */
    suspend fun createPart(
        name: String,
        sku: String,
        category: ItemCategory,
        basePrice: BigDecimal,
        minimumStock: BigDecimal,
        unitOfMeasure: String? = "UNIT"
    ): Result<Part>

    /** Actualiza un repuesto (`inventory:parts:manage`) y actualiza Room. */
    suspend fun updatePart(
        id: String,
        name: String,
        category: ItemCategory,
        basePrice: BigDecimal,
        minimumStock: BigDecimal,
        status: PartStatus? = null,
        unitOfMeasure: String? = null
    ): Result<Part>

    /** Lotes del repuesto (1 h; con [force] ignora la vigencia). Sin `inventory:batches:read` no hace nada. */
    suspend fun refreshBatches(itemId: String, force: Boolean = false): Result<Unit>

    fun observeBatches(itemId: String): Flow<List<Batch>>

    /** Ingreso físico directo de lote (`inventory:batches:receive`). */
    suspend fun receiveDirectBatch(
        itemId: String,
        batchNumber: String,
        quantity: BigDecimal,
        unitCost: BigDecimal,
        arrivalDate: Instant? = null,
        supplierId: String? = null
    ): Result<Batch>

    /** Descargo FIFO estricto (`inventory:batches:dispatch_fifo`). */
    suspend fun dispatchFifoBatch(
        itemId: String,
        quantity: BigDecimal,
        workOrderId: String? = null,
        taskId: String? = null
    ): Result<StockAllocation>

    /** Reintegro de lote (`inventory:batches:restore`). */
    suspend fun restoreBatch(
        itemId: String,
        quantity: BigDecimal,
        batchId: String? = null,
        reason: String? = null,
        workOrderId: String? = null
    ): Result<BatchRestoreResult>

    /** Bandeja de alertas en línea; sin red se deriva de la caché `parts`. */
    suspend fun getStockAlerts(): Result<List<StockAlert>>

    /** Repuestos en el mínimo o debajo según la caché: se actualiza solo al recargar el catálogo. */
    fun observeLowStock(): Flow<List<Part>>

    suspend fun getValuation(): Result<InventoryValuation>

    /** Proveedores (`inventory:suppliers:manage`). */
    suspend fun getSuppliers(search: String? = null, status: String? = null): Result<List<Supplier>>

    suspend fun createSupplier(
        businessName: String,
        taxId: String,
        contactName: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null
    ): Result<Supplier>

    suspend fun updateSupplier(
        id: String,
        businessName: String? = null,
        contactName: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        active: Boolean? = null
    ): Result<Supplier>

    suspend fun getPurchaseOrders(status: PurchaseOrderStatus?): Result<List<PurchaseOrder>>

    suspend fun getPurchaseOrder(id: String): Result<PurchaseOrder>

    /** Crea orden en borrador (`inventory:purchase_orders:create`) en la sede de la sesión. */
    suspend fun createPurchaseOrder(
        supplierId: String,
        items: List<PurchaseOrderLineInput>,
        orderNumber: String? = null,
        expectedDeliveryDate: String? = null,
        notes: String? = null
    ): Result<PurchaseOrder>

    /** Agrega una línea a una orden en borrador (`inventory:purchase_orders:create`). */
    suspend fun addPurchaseOrderItem(
        purchaseOrderId: String,
        itemId: String,
        quantity: BigDecimal,
        unitCost: BigDecimal
    ): Result<PurchaseOrder>

    /** Emite un borrador al proveedor (`DRAFT` → `ISSUED`). */
    suspend fun issuePurchaseOrder(id: String): Result<PurchaseOrder>

    /** Anula una orden pendiente (`CANCELED`). */
    suspend fun cancelPurchaseOrder(id: String, reason: String? = null): Result<PurchaseOrder>

    /**
     * Sube el comprobante y recibe la orden; luego recarga el catálogo y los lotes recibidos. Si la orden ya estaba
     * `RECEIVED` responde éxito sin subir nada (H15).
     */
    suspend fun receivePurchaseOrder(id: String, receiptNumber: String, photo: File, receivedAt: Instant?): Result<PurchaseOrder>
}
