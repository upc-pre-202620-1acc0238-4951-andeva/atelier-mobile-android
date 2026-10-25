package com.andeva.atelier.mobile.features.diagnostics.domain

import java.math.BigDecimal
import java.time.Instant

/** Resumen de un informe de salud con IA (`HealthReportCreatedResponse`, US21 E1). */
data class HealthReport(
    val reportId: String,
    val vehicleId: String,
    val overallHealthScore: Int?,
    val executiveSummary: String?,
    val totalRisksDetected: Int?,
    val generatedAt: Instant?
)

/** Informe completo (`HealthReportDetailResponse`): lo que el técnico contrasta con su criterio (US21 E1). */
data class HealthReportDetail(
    val reportId: String,
    val vehicleId: String,
    val daysAnalyzed: Int?,
    val generatedAt: Instant?,
    val diagnosis: HealthDiagnosis?
)

/** `VehicleHealthReportAiDto`. */
data class HealthDiagnosis(
    val healthScore: Int?,
    val overallCondition: String?,
    val executiveSummary: String?,
    val subsystemEvaluations: List<SubsystemEvaluation>,
    val predictiveRisks: List<PredictiveRisk>,
    val recommendedActions: List<RecommendedServiceAction>,
    val dtcCorrelations: List<DtcCorrelation>
)

data class SubsystemEvaluation(
    val subsystemName: String?,
    val status: String?,
    val score: Int?,
    val findings: String?
)

data class PredictiveRisk(
    val category: String?,
    val probability: Double?,
    val timeHorizonDays: Int?,
    val consequences: String?
)

/** Servicio sugerido por el modelo. La API no indica la moneda de [estimatedCost]. */
data class RecommendedServiceAction(
    val serviceCode: String?,
    val serviceName: String?,
    val urgency: String?,
    val estimatedCost: BigDecimal?,
    val rationale: String?
)

data class DtcCorrelation(
    val dtcCode: String?,
    val correlatedSensorReading: String?,
    val anomalyExplanation: String?
)
