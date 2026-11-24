package com.andeva.atelier.mobile.features.bays.infrastructure.remote

import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Bahías y asignaciones de docs/api/operations.md. */
interface WorkBayService {
    @GET("api/v1/work-bays")
    suspend fun getBays(@Query("branchId") branchId: String): Response<List<WorkBayDto>>

    @POST("api/v1/work-bays")
    suspend fun createBay(@Body body: CreateWorkBayRequestDto): Response<WorkBayDto>

    @PUT("api/v1/work-orders/{workOrderId}/bay")
    suspend fun assignBay(
        @Path("workOrderId") workOrderId: String,
        @Body body: AssignWorkBayRequestDto
    ): Response<WorkOrderDto>

    @DELETE("api/v1/work-orders/{workOrderId}/bay")
    suspend fun releaseBay(@Path("workOrderId") workOrderId: String): Response<Unit>

    @PUT("api/v1/work-bays/{bayId}/maintenance")
    suspend fun setMaintenance(
        @Path("bayId") bayId: String,
        @Body body: MaintenanceBayRequestDto
    ): Response<WorkBayDto>

    @PUT("api/v1/work-bays/{bayId}/restore")
    suspend fun restoreBay(@Path("bayId") bayId: String): Response<WorkBayDto>

    @PUT("api/v1/tasks/{taskId}/mechanic")
    suspend fun assignMechanic(
        @Path("taskId") taskId: String,
        @Body body: AssignTaskMechanicRequestDto
    ): Response<WorkOrderTaskDto>
}
