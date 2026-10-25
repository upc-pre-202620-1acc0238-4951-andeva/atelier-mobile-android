package com.andeva.atelier.mobile.features.cashier.infrastructure.remote

import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Pago formal, pase de salida y entrega de docs/api/operations.md. */
interface HandoverService {
    @POST("api/v1/work-orders/{workOrderId}/mark-as-paid")
    suspend fun markAsPaid(@Path("workOrderId") workOrderId: String): Response<WorkOrderDto>

    @Streaming
    @GET("api/v1/work-orders/{workOrderId}/exit-pass/pdf")
    suspend fun getExitPassPdf(@Path("workOrderId") workOrderId: String): Response<ResponseBody>

    @POST("api/v1/work-orders/{workOrderId}/deliver")
    suspend fun deliver(@Path("workOrderId") workOrderId: String): Response<WorkOrderDto>
}
