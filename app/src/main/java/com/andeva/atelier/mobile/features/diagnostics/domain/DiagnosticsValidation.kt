package com.andeva.atelier.mobile.features.diagnostics.domain

/** Restricciones de docs/api/iot.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object DiagnosticsValidation {
    const val MIN_DAYS_TO_ANALYZE = 7
    const val MAX_DAYS_TO_ANALYZE = 90

    /** `GenerateHealthReportRequest.daysToAnalyze`: 7–90. */
    fun isDaysToAnalyze(days: Int) = days in MIN_DAYS_TO_ANALYZE..MAX_DAYS_TO_ANALYZE

    /** `currentOdometerKm` / `finalOdometerKm`: ≥ 0 o sin enviar. */
    fun isOdometer(km: Int?) = km == null || km >= 0
}
