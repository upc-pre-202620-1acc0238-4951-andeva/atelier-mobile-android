package com.andeva.atelier.mobile.features.workorders.infrastructure.remote

import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Órdenes de trabajo de docs/api/operations.md (sin bahía, entrega ni pago: pasos 9). */
interface WorkOrderService {
    @GET("api/v1/work-orders")
    suspend fun getWorkOrders(@Query("branchId") branchId: String): Response<List<WorkOrderDto>>

    @GET("api/v1/work-orders/{workOrderId}")
    suspend fun getWorkOrder(@Path("workOrderId") id: String): Response<WorkOrderDto>

    @POST("api/v1/work-orders")
    suspend fun create(@Body body: CreateWorkOrderRequestDto): Response<WorkOrderDto>

    @PUT("api/v1/work-orders/{workOrderId}")
    suspend fun update(@Path("workOrderId") id: String, @Body body: UpdateWorkOrderRequestDto): Response<WorkOrderDto>

    @POST("api/v1/work-orders/{workOrderId}/tasks")
    suspend fun addTask(@Path("workOrderId") id: String, @Body body: CreateWorkOrderTaskRequestDto): Response<WorkOrderTaskDto>

    @PUT("api/v1/tasks/{taskId}")
    suspend fun updateTask(@Path("taskId") taskId: String, @Body body: UpdateWorkOrderTaskRequestDto): Response<WorkOrderTaskDto>

    @POST("api/v1/work-orders/{workOrderId}/intake-images")
    suspend fun attachIntakeImage(@Path("workOrderId") id: String, @Body body: AttachImageRequestDto): Response<WorkOrderImageDto>

    @POST("api/v1/work-orders/{workOrderId}/proposals")
    suspend fun submitProposal(@Path("workOrderId") id: String, @Body body: SubmitProposalRequestDto): Response<TaskProposalDto>

    @POST("api/v1/work-orders/{workOrderId}/proposals/{proposalId}/notify")
    suspend fun notifyProposal(@Path("workOrderId") id: String, @Path("proposalId") proposalId: String): Response<EmailSentDto>

    @POST("api/v1/work-orders/{workOrderId}/proposals/{proposalId}/approve")
    suspend fun approveProposal(
        @Path("workOrderId") id: String,
        @Path("proposalId") proposalId: String,
        @Body body: ApproveProposalRequestDto
    ): Response<WorkOrderTaskDto>

    @POST("api/v1/work-orders/{workOrderId}/proposals/{proposalId}/reject")
    suspend fun rejectProposal(
        @Path("workOrderId") id: String,
        @Path("proposalId") proposalId: String,
        @Body body: RejectProposalRequestDto
    ): Response<TaskProposalDto>

    @Streaming
    @GET("api/v1/work-orders/{workOrderId}/quotation/pdf")
    suspend fun getQuotationPdf(@Path("workOrderId") id: String): Response<ResponseBody>

    @POST("api/v1/work-orders/{workOrderId}/quotation/send")
    suspend fun sendQuotation(@Path("workOrderId") id: String): Response<EmailSentDto>

    @POST("api/v1/work-orders/{workOrderId}/quotation/approve")
    suspend fun approveQuotation(@Path("workOrderId") id: String): Response<WorkOrderDto>

    @POST("api/v1/work-orders/{workOrderId}/quotation/reject")
    suspend fun rejectQuotation(@Path("workOrderId") id: String, @Body body: ReasonRequestDto): Response<WorkOrderDto>

    @GET("api/v1/work-orders/{workOrderId}/part-movements")
    suspend fun getPartMovements(@Path("workOrderId") id: String): Response<List<PartMovementDto>>

    @GET("api/v1/work-orders/{workOrderId}/profitability")
    suspend fun getProfitability(@Path("workOrderId") id: String): Response<WorkOrderProfitabilityDto>
}

/** Catálogo de servicios del taller (`operations:services:read`). */
interface ServiceCatalogService {
    @GET("api/v1/services")
    suspend fun getServices(): Response<List<ServiceDto>>
}
