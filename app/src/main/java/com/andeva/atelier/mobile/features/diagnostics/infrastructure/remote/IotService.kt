package com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * IoT de docs/api/iot.md que usa la app (US16–US21, US43 E1). Registrar dispositivos, `generate-async` (sin endpoint
 * para consultar el trabajo), telemetría histórica, alertas del taller y convertir alertas en citas no tienen historia
 * móvil.
 */
interface IotService {
    @GET("api/v1/iot/devices")
    suspend fun getDevices(@Query("status") status: String?): Response<List<ObdDeviceDto>>

    /** 404 = el vehículo no tiene escáner instalado. */
    @GET("api/v1/iot/installations/vehicle/{vehicleId}/active")
    suspend fun getActiveInstallation(@Path("vehicleId") vehicleId: String): Response<DeviceInstallationDto>

    /** 409 si el dispositivo o el vehículo ya tienen una instalación activa. */
    @POST("api/v1/iot/installations/install")
    suspend fun install(@Body body: InstallDeviceRequestDto): Response<DeviceInstallationDto>

    @POST("api/v1/iot/installations/{id}/uninstall")
    suspend fun uninstall(@Path("id") id: String, @Body body: UninstallDeviceRequestDto): Response<DeviceInstallationDto>

    /** Si el vehículo ya tiene ese DTC activo devuelve esa falla (R3 de docs/06-offline-sync.md §7). */
    @POST("api/v1/iot/faults")
    suspend fun registerFault(@Body body: RegisterFaultRequestDto): Response<VehicleFaultDto>

    @GET("api/v1/iot/faults/vehicle/{vehicleId}/active")
    suspend fun getActiveFaults(@Path("vehicleId") vehicleId: String): Response<List<VehicleFaultDto>>

    /** Resolver dos veces responde 200. */
    @POST("api/v1/iot/faults/{id}/resolve")
    suspend fun resolveFault(@Path("id") id: String): Response<VehicleFaultDto>

    /** 202. Lecturas repetidas (vehículo + `timestamp`) se ignoran; 404 sin instalación activa. */
    @POST("api/v1/iot/telemetry/batch")
    suspend fun ingestTelemetry(@Body body: TelemetryBatchRequestDto): Response<TelemetryIngestionAckDto>

    /** 201. 422 `ERR_INSUFFICIENT_TELEMETRY`, 403 cuota o plan, 502 IA no disponible. */
    @POST("api/v1/iot/health-reports/generate")
    suspend fun generateHealthReport(
        @Query("vehicleId") vehicleId: String,
        @Body body: GenerateHealthReportRequestDto
    ): Response<HealthReportCreatedDto>

    /** 404 = sin informes previos. */
    @GET("api/v1/iot/health-reports/latest")
    suspend fun getLatestHealthReport(@Query("vehicleId") vehicleId: String): Response<HealthReportCreatedDto>

    @GET("api/v1/iot/health-reports/{reportId}")
    suspend fun getHealthReport(@Path("reportId") reportId: String): Response<HealthReportDetailDto>

    @Streaming
    @GET("api/v1/iot/health-reports/{reportId}/pdf")
    suspend fun getHealthReportPdf(@Path("reportId") reportId: String): Response<ResponseBody>

    @GET("api/v1/iot/alerts/vehicle/{vehicleId}")
    suspend fun getVehicleAlerts(@Path("vehicleId") vehicleId: String): Response<List<PredictiveAlertDto>>

    @PATCH("api/v1/iot/alerts/{id}/acknowledge")
    suspend fun acknowledgeAlert(@Path("id") id: String, @Body body: AcknowledgeAlertRequestDto): Response<PredictiveAlertDto>
}
