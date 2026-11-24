package com.andeva.atelier.mobile.features.diagnostics.presentation.report

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportDetail
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import java.io.File

enum class PdfActionMode { OPEN, SHARE }

data class AiHealthReportUiState(
    val vehicleId: String = "",
    val plate: String? = null,
    val vehicle: Vehicle? = null,
    val planIncludesAi: Boolean = true,
    val canGenerate: Boolean = false,
    val canRead: Boolean = false,
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isDownloadingPdf: Boolean = false,
    val latestSummary: HealthReport? = null,
    val detail: HealthReportDetail? = null,
    val generateSheetOpen: Boolean = false,
    val daysToAnalyze: Int = 30,
    val includeResolvedDtcHistory: Boolean = true,
    val pendingPdfFile: File? = null,
    val pdfActionMode: PdfActionMode = PdfActionMode.OPEN,
    val error: UiText? = null
) {
    val displayPlate: String get() = vehicle?.plate ?: plate.orEmpty()

    val effectiveScore: Int?
        get() = detail?.diagnosis?.healthScore ?: latestSummary?.overallHealthScore

    val effectiveSummary: String?
        get() = detail?.diagnosis?.executiveSummary ?: latestSummary?.executiveSummary
}
