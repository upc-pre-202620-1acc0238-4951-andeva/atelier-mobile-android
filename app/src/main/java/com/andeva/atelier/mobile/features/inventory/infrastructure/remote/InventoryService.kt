package com.andeva.atelier.mobile.features.inventory.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de inventario de docs/api/inventory.md (catálogo, alta/edición de repuestos, lotes FIFO,
 * alertas, valorización, proveedores y ciclo completo de órdenes de compra).
 */
interface InventoryService {
    /** Array paginado en memoria (sin total): para el catálogo completo se piden páginas hasta recibir menos de [size]. */
    @GET("api/v1/inventory/items")
    suspend fun getItems(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): Response<List<PartDto>>

    @POST("api/v1/inventory/items")
    suspend fun createItem(@Body body: CreatePartRequestDto): Response<PartDto>

    @GET("api/v1/inventory/items/{id}")
    suspend fun getItem(@Path("id") id: String): Response<PartDetailDto>

    @PUT("api/v1/inventory/items/{id}")
    suspend fun updateItem(@Path("id") id: String, @Body body: UpdatePartRequestDto): Response<PartDto>

    /** 404 si no existe; 400 si el SKU no cumple el formato. */
    @GET("api/v1/inventory/items/by-sku/{sku}")
    suspend fun getBySku(@Path("sku") sku: String): Response<PartDto>

    /** Todos los lotes, del más nuevo al más viejo y también agotados. */
    @GET("api/v1/inventory/items/{id}/batches")
    suspend fun getBatches(@Path("id") id: String): Response<List<BatchDto>>

    @POST("api/v1/inventory/batches/receive")
    suspend fun receiveBatch(@Body body: ReceiveBatchRequestDto): Response<BatchDto>

    @POST("api/v1/inventory/batches/dispatch")
    suspend fun dispatchBatchFifo(@Body body: DispatchBatchFifoRequestDto): Response<StockAllocationDto>

    @POST("api/v1/inventory/batches/restore")
    suspend fun restoreBatch(@Body body: RestoreBatchRequestDto): Response<RestoreBatchResponseDto>

    @GET("api/v1/inventory/alerts")
    suspend fun getAlerts(): Response<List<StockAlertDto>>

    @GET("api/v1/inventory/items/valuation")
    suspend fun getValuation(): Response<ValuationDto>

    @GET("api/v1/inventory/suppliers")
    suspend fun getSuppliers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Response<List<SupplierDto>>

    @POST("api/v1/inventory/suppliers")
    suspend fun createSupplier(@Body body: RegisterSupplierRequestDto): Response<SupplierDto>

    @PUT("api/v1/inventory/suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: String, @Body body: UpdateSupplierRequestDto): Response<SupplierDto>

    @GET("api/v1/inventory/purchase-orders")
    suspend fun getPurchaseOrders(@Query("status") status: String?): Response<List<PurchaseOrderDto>>

    @POST("api/v1/inventory/purchase-orders")
    suspend fun createPurchaseOrder(@Body body: CreatePurchaseOrderRequestDto): Response<PurchaseOrderDto>

    @GET("api/v1/inventory/purchase-orders/{id}")
    suspend fun getPurchaseOrder(@Path("id") id: String): Response<PurchaseOrderDto>

    @POST("api/v1/inventory/purchase-orders/{id}/items")
    suspend fun addPurchaseOrderItem(
        @Path("id") id: String,
        @Body body: CreatePurchaseOrderItemRequestDto
    ): Response<PurchaseOrderDto>

    @PUT("api/v1/inventory/purchase-orders/{id}/issue")
    suspend fun issuePurchaseOrder(@Path("id") id: String): Response<PurchaseOrderDto>

    @PUT("api/v1/inventory/purchase-orders/{id}/cancel")
    suspend fun cancelPurchaseOrder(
        @Path("id") id: String,
        @Body body: CancelPurchaseOrderRequestDto = CancelPurchaseOrderRequestDto()
    ): Response<PurchaseOrderDto>

    /** Solo `ISSUED`. Todo error es 400, también si ya estaba recibida (H15). */
    @PUT("api/v1/inventory/purchase-orders/{id}/receive")
    suspend fun receivePurchaseOrder(@Path("id") id: String, @Body body: ReceivePurchaseOrderRequestDto): Response<PurchaseOrderDto>
}
