package com.andeva.atelier.mobile.features.tasks.infrastructure.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/*
 * Lecturas de otros contextos que necesita `tasks` antes de que exista su feature: bahía (operations, paso 9). Ese
 * paso puede moverla a su feature, como se hizo con la placa (`VehicleService` en `reception`, paso 6), el catálogo
 * (`InventoryService` en `inventory`, paso 7) y `EmployeeService` → `core/data/remote/employee/`.
 */

/** `GET /work-bays?branchId=` de docs/api/operations.md. */
interface WorkBayLookupService {
    @GET("api/v1/work-bays")
    suspend fun getBays(@Query("branchId") branchId: String): Response<List<WorkBayDto>>
}

/** `WorkBayResource`. */
data class WorkBayDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("bayType") val bayType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("currentWorkOrderId") val currentWorkOrderId: String?
)
