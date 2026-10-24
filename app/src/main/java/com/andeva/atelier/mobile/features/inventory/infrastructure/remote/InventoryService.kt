package com.andeva.atelier.mobile.features.inventory.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Inventario de docs/api/inventory.md que usa la app (catálogo, lotes, alertas, valorización y recepción de compras).
 * Altas y ediciones de repuestos, proveedores y órdenes de compra no tienen historia móvil.
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

    @GET("api/v1/inventory/items/{id}")
    suspend fun getItem(@Path("id") id: String): Response<PartDetailDto>

    /** 404 si no existe; 400 si el SKU no cumple el formato. */
    @GET("api/v1/inventory/items/by-sku/{sku}")
    suspend fun getBySku(@Path("sku") sku: String): Response<PartDto>

    /** Todos los lotes, del más nuevo al más viejo y también agotados. */
    @GET("api/v1/inventory/items/{id}/batches")
    suspend fun getBatches(@Path("id") id: String): Response<List<BatchDto>>

    @GET("api/v1/inventory/alerts")
    suspend fun getAlerts(): Response<List<StockAlertDto>>

    @GET("api/v1/inventory/items/valuation")
    suspend fun getValuation(): Response<ValuationDto>

    @GET("api/v1/inventory/purchase-orders")
    suspend fun getPurchaseOrders(@Query("status") status: String?): Response<List<PurchaseOrderDto>>

    @GET("api/v1/inventory/purchase-orders/{id}")
    suspend fun getPurchaseOrder(@Path("id") id: String): Response<PurchaseOrderDto>

    /** Solo `ISSUED`. Todo error es 400, también si ya estaba recibida (H15). */
    @PUT("api/v1/inventory/purchase-orders/{id}/receive")
    suspend fun receivePurchaseOrder(@Path("id") id: String, @Body body: ReceivePurchaseOrderRequestDto): Response<PurchaseOrderDto>
}
