package com.andeva.atelier.mobile.features.reports.infrastructure.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Flujo de caja (docs/api/invoicing.md) y flota (docs/api/iot.md). `from`/`to` son yyyy-MM-dd. */
interface ReportService {
    @GET("api/v1/invoicing/financial-reports/cash-flow")
    suspend fun getCashFlow(@Query("from") from: String, @Query("to") to: String): Response<CashFlowSummaryDto>

    @Streaming
    @GET("api/v1/invoicing/financial-reports/cash-flow/pdf")
    suspend fun getCashFlowPdf(@Query("from") from: String, @Query("to") to: String): Response<ResponseBody>

    @Streaming
    @GET("api/v1/iot/health-reports/fleet/pdf")
    suspend fun getFleetHealthPdf(@Query("customerId") customerId: String): Response<ResponseBody>
}
