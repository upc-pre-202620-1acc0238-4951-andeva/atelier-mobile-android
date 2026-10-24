package com.andeva.atelier.mobile.features.tasks.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Endpoints de tareas de docs/api/operations.md. */
interface TaskService {
    /** Tareas abiertas del mecánico autenticado, en curso primero. */
    @GET("api/v1/tasks/my")
    suspend fun getMyTasks(): Response<List<AssignedTaskDto>>

    @GET("api/v1/tasks/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): Response<WorkOrderTaskDto>

    /** Inicia o reanuda tras una pausa propia. */
    @POST("api/v1/tasks/{taskId}/start")
    suspend fun start(@Path("taskId") taskId: String): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/pause")
    suspend fun pause(@Path("taskId") taskId: String, @Body body: PauseTaskRequestDto): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/hold")
    suspend fun requestHold(@Path("taskId") taskId: String, @Body body: HoldTaskRequestDto): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/hold/approve")
    suspend fun approveHold(@Path("taskId") taskId: String): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/hold/reject")
    suspend fun rejectHold(@Path("taskId") taskId: String): Response<WorkOrderTaskDto>

    /** Levanta la suspensión cuando llega el repuesto (el mecánico no puede con `/start`). */
    @POST("api/v1/tasks/{taskId}/resume")
    suspend fun resume(@Path("taskId") taskId: String): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/complete")
    suspend fun complete(@Path("taskId") taskId: String, @Body body: CompleteTaskRequestDto): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/evidence-images")
    suspend fun attachEvidence(
        @Path("taskId") taskId: String,
        @Body body: AttachTaskEvidenceRequestDto
    ): Response<WorkOrderTaskImageDto>

    @POST("api/v1/tasks/{taskId}/products")
    suspend fun addProduct(@Path("taskId") taskId: String, @Body body: AddTaskProductRequestDto): Response<TaskProductDto>

    @PUT("api/v1/tasks/{taskId}/products/{productId}")
    suspend fun updateProduct(
        @Path("taskId") taskId: String,
        @Path("productId") productId: String,
        @Body body: UpdateTaskProductRequestDto
    ): Response<TaskProductDto>

    /** 204 sin cuerpo: usar `apiCallUnit`. */
    @DELETE("api/v1/tasks/{taskId}/products/{productId}")
    suspend fun removeProduct(@Path("taskId") taskId: String, @Path("productId") productId: String): Response<Unit>
}
