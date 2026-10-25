package com.andeva.atelier.mobile.features.cashier.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Comprobantes, pagos y series de docs/api/invoicing.md. */
interface CashierService {
    @POST("api/v1/invoicing/vouchers")
    suspend fun issueVoucher(@Body body: IssueVoucherRequestDto): Response<ElectronicVoucherDto>

    @GET("api/v1/invoicing/vouchers/{id}")
    suspend fun getVoucher(@Path("id") id: String): Response<ElectronicVoucherDto>

    @GET("api/v1/invoicing/vouchers/work-order/{workOrderId}")
    suspend fun getWorkOrderVouchers(@Path("workOrderId") workOrderId: String): Response<List<ElectronicVoucherDto>>

    @POST("api/v1/invoicing/payments")
    suspend fun registerPayment(@Body body: RegisterPaymentRequestDto): Response<VoucherPaymentDto>

    @GET("api/v1/invoicing/payments/voucher/{voucherId}")
    suspend fun getVoucherPayments(@Path("voucherId") voucherId: String): Response<List<VoucherPaymentDto>>

    @GET("api/v1/invoicing/payments/branch/{branchId}/daily")
    suspend fun getDailyReconciliation(
        @Path("branchId") branchId: String,
        @Query("date") date: String?
    ): Response<DailyReconciliationDto>

    @GET("api/v1/invoicing/series-configurations/branch/{branchId}")
    suspend fun getBranchSeries(@Path("branchId") branchId: String): Response<List<SeriesConfigurationDto>>

    @POST("api/v1/invoicing/series-configurations")
    suspend fun configureSeries(@Body body: ConfigureSeriesRequestDto): Response<SeriesConfigurationDto>

    @PUT("api/v1/invoicing/series-configurations/{id}/status")
    suspend fun updateSeriesStatus(
        @Path("id") id: String,
        @Body body: UpdateSeriesStatusRequestDto
    ): Response<Unit>
}
