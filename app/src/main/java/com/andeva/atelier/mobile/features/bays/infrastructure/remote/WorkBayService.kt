package com.andeva.atelier.mobile.features.bays.infrastructure.remote

import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Bahías y asignaciones de docs/api/operations.md. */
interface WorkBayService {
    @GET("api/v1/work-bays")
    suspend fun getBays(@Query("branchId") branchId: String): Response<List<WorkBayDto>>

    @PUT("api/v1/work-orders/{workOrderId}/bay")
    suspend fun assignBay(
        @Path("workOrderId") workOrderId: String,
        @Body body: AssignWorkBayRequestDto
    ): Response<WorkOrderDto>

    @PUT("api/v1/tasks/{taskId}/mechanic")
    suspend fun assignMechanic(
        @Path("taskId") taskId: String,
        @Body body: AssignTaskMechanicRequestDto
    ): Response<WorkOrderTaskDto>
}
