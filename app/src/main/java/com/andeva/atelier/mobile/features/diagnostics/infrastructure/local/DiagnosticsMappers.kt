package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceInstallation
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.DtcCorrelation
import com.andeva.atelier.mobile.features.diagnostics.domain.FaultSeverity
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthDiagnosis
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportDetail
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdDevice
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertType
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveRisk
import com.andeva.atelier.mobile.features.diagnostics.domain.RecommendedServiceAction
import com.andeva.atelier.mobile.features.diagnostics.domain.SubsystemEvaluation
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.DeviceInstallationDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.HealthReportCreatedDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.HealthReportDetailDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.ObdDeviceDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.PredictiveAlertDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.TelemetryReadingItemDto
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.VehicleFaultDto
import java.time.Instant

/** Instantes del backend en ISO-8601 UTC (docs/api/README.md §6); Room los guarda como epoch ms. */
private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

private inline fun <reified T : Enum<T>> String?.toEnum(): T? = this?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

fun VehicleFaultDto.toEntity(fallbackVehicleId: String, syncedAt: Long) = VehicleFaultEntity(
    id = id,
    vehicleId = vehicleId ?: fallbackVehicleId,
    dtcCode = dtcCode.orEmpty(),
    severity = severity.orEmpty(),
    description = description.orEmpty(),
    detectedAt = detectedAt.toInstantOrNull()?.toEpochMilli() ?: syncedAt,
    isResolved = isResolved == true,
    resolvedAt = resolvedAt.toInstantOrNull()?.toEpochMilli(),
    syncedAt = syncedAt
)

fun VehicleFaultEntity.toDomain() = VehicleFault(
    id = id,
    vehicleId = vehicleId,
    dtc = Dtc.of(dtcCode),
    dtcCode = dtcCode,
    severity = severity.toEnum<FaultSeverity>(),
    description = description,
    detectedAt = Instant.ofEpochMilli(detectedAt),
    isResolved = isResolved,
    resolvedAt = resolvedAt?.let(Instant::ofEpochMilli)
)

fun TelemetryReadingEntity.toItemDto() = TelemetryReadingItemDto(
    timestamp = Instant.ofEpochMilli(timestamp).toString(),
    engineTempCelsius = engineTempCelsius,
    engineRpm = engineRpm,
    speedKmh = speedKmh,
    batteryVoltage = batteryVoltage,
    fuelPercentage = fuelPercentage,
    latitude = latitude,
    longitude = longitude
)

fun PredictiveAlertDto.toEntity(fallbackVehicleId: String, syncedAt: Long) = PredictiveAlertEntity(
    id = id,
    vehicleId = vehicleId ?: fallbackVehicleId,
    alertType = alertType.orEmpty(),
    confidenceScore = confidenceScore ?: 0.0,
    message = message.orEmpty(),
    status = status.orEmpty(),
    createdAt = createdAt.toInstantOrNull()?.toEpochMilli() ?: syncedAt,
    syncedAt = syncedAt
)

fun PredictiveAlertEntity.toDomain() = PredictiveAlert(
    id = id,
    vehicleId = vehicleId,
    alertType = alertType.toEnum<PredictiveAlertType>(),
    confidenceScore = confidenceScore,
    message = message,
    status = status.toEnum<PredictiveAlertStatus>(),
    recommendedServiceId = null,
    createdAt = Instant.ofEpochMilli(createdAt)
)

fun PredictiveAlertDto.toDomain(fallbackVehicleId: String) = PredictiveAlert(
    id = id,
    vehicleId = vehicleId ?: fallbackVehicleId,
    alertType = alertType.toEnum<PredictiveAlertType>(),
    confidenceScore = confidenceScore ?: 0.0,
    message = message.orEmpty(),
    status = status.toEnum<PredictiveAlertStatus>(),
    recommendedServiceId = recommendedServiceId,
    createdAt = createdAt.toInstantOrNull()
)

fun ObdDeviceDto.toDomain() = ObdDevice(
    id = id,
    deviceIdentifier = deviceIdentifier.orEmpty(),
    connectionType = connectionType,
    status = status.toEnum<DeviceStatus>(),
    hardwareModel = hardwareModel,
    firmwareVersion = firmwareVersion
)

fun DeviceInstallationDto.toDomain() = DeviceInstallation(
    id = id,
    deviceId = deviceId.orEmpty(),
    vehicleId = vehicleId.orEmpty(),
    installedAt = installedAt.toInstantOrNull(),
    uninstalledAt = uninstalledAt.toInstantOrNull(),
    initialOdometerKm = initialOdometerKm,
    finalOdometerKm = finalOdometerKm,
    isActive = isActive == true
)

fun HealthReportCreatedDto.toDomain(fallbackVehicleId: String) = HealthReport(
    reportId = reportId,
    vehicleId = vehicleId ?: fallbackVehicleId,
    overallHealthScore = overallHealthScore,
    executiveSummary = executiveSummary,
    totalRisksDetected = totalRisksDetected,
    generatedAt = generatedAt.toInstantOrNull()
)

fun HealthReportDetailDto.toDomain() = HealthReportDetail(
    reportId = reportId,
    vehicleId = vehicleId.orEmpty(),
    daysAnalyzed = daysAnalyzed,
    generatedAt = generatedAt.toInstantOrNull(),
    diagnosis = diagnosis?.let { ai ->
        HealthDiagnosis(
            healthScore = ai.healthScore,
            overallCondition = ai.overallCondition,
            executiveSummary = ai.executiveSummary,
            subsystemEvaluations = ai.subsystemEvaluations.orEmpty().map { SubsystemEvaluation(it.subsystemName, it.status, it.score, it.findings) },
            predictiveRisks = ai.predictiveRisks.orEmpty().map { PredictiveRisk(it.category, it.probability, it.timeHorizonDays, it.consequences) },
            recommendedActions = ai.recommendedActions.orEmpty().map {
                RecommendedServiceAction(it.serviceCode, it.serviceName, it.urgency, it.estimatedCost, it.rationale)
            },
            dtcCorrelations = ai.dtcCorrelations.orEmpty().map { DtcCorrelation(it.dtcCode, it.correlatedSensorReading, it.anomalyExplanation) }
        )
    }
)
