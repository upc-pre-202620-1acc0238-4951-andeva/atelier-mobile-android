package com.andeva.atelier.mobile.features.tasks.infrastructure.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal

/*
 * Lecturas de otros contextos que necesita `tasks` antes de que existan sus features: bahía (operations, paso 9)
 * y catálogo de repuestos (inventory, paso 7). Esos pasos pueden moverlas a su feature, como se hizo con la placa
 * (`VehicleService` en `reception`, paso 6) y `EmployeeService` → `core/data/remote/employee/`.
 */

/** `GET /inventory/items?search=` de docs/api/inventory.md, en línea y sin caché (la caché `parts` es del paso 7). */
interface PartSearchService {
    @GET("api/v1/inventory/items")
    suspend fun search(@Query("search") search: String?): Response<List<PartDto>>
}

/** `GET /work-bays?branchId=` de docs/api/operations.md. */
interface WorkBayLookupService {
    @GET("api/v1/work-bays")
    suspend fun getBays(@Query("branchId") branchId: String): Response<List<WorkBayDto>>
}

/** `PartResource`. */
data class PartDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("sku") val sku: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("basePrice") val basePrice: BigDecimal?,
    @SerializedName("totalStock") val totalStock: BigDecimal?,
    @SerializedName("minimumStock") val minimumStock: BigDecimal?,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String?,
    @SerializedName("status") val status: String?
)

/** `WorkBayResource`. */
data class WorkBayDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("bayType") val bayType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("currentWorkOrderId") val currentWorkOrderId: String?
)
