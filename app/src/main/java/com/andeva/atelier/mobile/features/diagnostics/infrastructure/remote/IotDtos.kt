package com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** `Obd2DeviceResponse` de docs/api/iot.md. */
data class ObdDeviceDto(
    @SerializedName("id") val id: String,
    @SerializedName("tenantId") val tenantId: String?,
    @SerializedName("deviceIdentifier") val deviceIdentifier: String?,
    @SerializedName("connectionType") val connectionType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("hardwareModel") val hardwareModel: String?,
    @SerializedName("firmwareVersion") val firmwareVersion: String?,
    @SerializedName("createdAt") val createdAt: String?
)

/** `DeviceInstallationResponse`. */
data class DeviceInstallationDto(
    @SerializedName("id") val id: String,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("tenantId") val tenantId: String?,
    @SerializedName("installedAt") val installedAt: String?,
    @SerializedName("uninstalledAt") val uninstalledAt: String?,
    @SerializedName("initialOdometerKm") val initialOdometerKm: Int?,
    @SerializedName("finalOdometerKm") val finalOdometerKm: Int?,
    @SerializedName("isActive") val isActive: Boolean?
)

/** `InstallDeviceRequest`. */
data class InstallDeviceRequestDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("currentOdometerKm") val currentOdometerKm: Int?
)

/** `UninstallDeviceRequest`: sin `uninstalledAt` el backend usa su hora. */
data class UninstallDeviceRequestDto(
    @SerializedName("finalOdometerKm") val finalOdometerKm: Int?,
    @SerializedName("uninstalledAt") val uninstalledAt: String? = null
)

/** `RegisterVehicleFaultRequest`: payload de `REGISTER_FAULT`. */
data class RegisterFaultRequestDto(
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("dtcCode") val dtcCode: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String?
)

/** `VehicleFaultResponse`. */
data class VehicleFaultDto(
    @SerializedName("id") val id: String,
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("dtcCode") val dtcCode: String?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("detectedAt") val detectedAt: String?,
    @SerializedName("isResolved") val isResolved: Boolean?,
    @SerializedName("resolvedAt") val resolvedAt: String?
)

/** Payload de `RESOLVE_FAULT`: [faultId] puede ser `local-{uuid}` si se borró mientras su registro se enviaba. */
data class ResolveFaultPayload(
    @SerializedName("faultId") val faultId: String,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("dtcCode") val dtcCode: String
)

/** `TelemetryBatchRequest`: payload de `TELEMETRY_BATCH` (hasta 200 lecturas). */
data class TelemetryBatchRequestDto(
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("readings") val readings: List<TelemetryReadingItemDto>
)

/** `TelemetryReadingItemDto`: `timestamp` y `engineTempCelsius` son obligatorios. */
data class TelemetryReadingItemDto(
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("engineTempCelsius") val engineTempCelsius: Double,
    @SerializedName("engineRpm") val engineRpm: Int?,
    @SerializedName("speedKmh") val speedKmh: Int?,
    @SerializedName("batteryVoltage") val batteryVoltage: Double?,
    @SerializedName("fuelPercentage") val fuelPercentage: Double?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)

/** `TelemetryIngestionAckResponse`. */
data class TelemetryIngestionAckDto(
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("ingestedCount") val ingestedCount: Int?,
    @SerializedName("anomalyDetected") val anomalyDetected: Boolean?,
    @SerializedName("alertMessage") val alertMessage: String?
)

/** `GenerateHealthReportRequest`. */
data class GenerateHealthReportRequestDto(
    @SerializedName("daysToAnalyze") val daysToAnalyze: Int,
    @SerializedName("includeResolvedDtcHistory") val includeResolvedDtcHistory: Boolean?,
    @SerializedName("triggerReason") val triggerReason: String? = null
)

/** `HealthReportCreatedResponse`. */
data class HealthReportCreatedDto(
    @SerializedName("reportId") val reportId: String,
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("overallHealthScore") val overallHealthScore: Int?,
    @SerializedName("executiveSummary") val executiveSummary: String?,
    @SerializedName("totalRisksDetected") val totalRisksDetected: Int?,
    @SerializedName("generatedAt") val generatedAt: String?,
    @SerializedName("jsonResourceUrl") val jsonResourceUrl: String?,
    @SerializedName("pdfDownloadUrl") val pdfDownloadUrl: String?
)

/** `HealthReportDetailResponse`. */
data class HealthReportDetailDto(
    @SerializedName("reportId") val reportId: String,
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("daysAnalyzed") val daysAnalyzed: Int?,
    @SerializedName("generatedAt") val generatedAt: String?,
    @SerializedName("diagnosis") val diagnosis: VehicleHealthReportAiDto?,
    @SerializedName("pdfDownloadUrl") val pdfDownloadUrl: String?
)

/** `VehicleHealthReportAiDto`. */
data class VehicleHealthReportAiDto(
    @SerializedName("healthScore") val healthScore: Int?,
    @SerializedName("overallCondition") val overallCondition: String?,
    @SerializedName("executiveSummary") val executiveSummary: String?,
    @SerializedName("subsystemEvaluations") val subsystemEvaluations: List<SubsystemEvaluationDto>?,
    @SerializedName("predictiveRisks") val predictiveRisks: List<PredictiveRiskDto>?,
    @SerializedName("recommendedActions") val recommendedActions: List<RecommendedServiceActionDto>?,
    @SerializedName("dtcCorrelations") val dtcCorrelations: List<DtcTelemetryCorrelationDto>?
)

data class SubsystemEvaluationDto(
    @SerializedName("subsystemName") val subsystemName: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("score") val score: Int?,
    @SerializedName("findings") val findings: String?
)

data class PredictiveRiskDto(
    @SerializedName("category") val category: String?,
    @SerializedName("probability") val probability: Double?,
    @SerializedName("timeHorizonDays") val timeHorizonDays: Int?,
    @SerializedName("consequences") val consequences: String?
)

data class RecommendedServiceActionDto(
    @SerializedName("serviceCode") val serviceCode: String?,
    @SerializedName("serviceName") val serviceName: String?,
    @SerializedName("urgency") val urgency: String?,
    @SerializedName("estimatedCost") val estimatedCost: BigDecimal?,
    @SerializedName("rationale") val rationale: String?
)

data class DtcTelemetryCorrelationDto(
    @SerializedName("dtcCode") val dtcCode: String?,
    @SerializedName("correlatedSensorReading") val correlatedSensorReading: String?,
    @SerializedName("anomalyExplanation") val anomalyExplanation: String?
)

/** `PredictiveAlertResponse`. */
data class PredictiveAlertDto(
    @SerializedName("id") val id: String,
    @SerializedName("vehicleId") val vehicleId: String?,
    @SerializedName("recommendedServiceId") val recommendedServiceId: String?,
    @SerializedName("alertType") val alertType: String?,
    @SerializedName("confidenceScore") val confidenceScore: Double?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("createdAt") val createdAt: String?
)

/** `AcknowledgeAlertRequest` (opcional): se envía vacío. */
data class AcknowledgeAlertRequestDto(
    @SerializedName("acknowledgedBy") val acknowledgedBy: String? = null
)
