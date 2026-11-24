package com.andeva.atelier.mobile.features.diagnostics.presentation.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.application.DownloadHealthReportPdfUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GenerateHealthReportUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetHealthReportDetailUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetLatestHealthReportUseCase
import com.andeva.atelier.mobile.features.diagnostics.presentation.toDiagnosticsUiText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveVehicleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiHealthReportViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeVehicle: ObserveVehicleUseCase,
    connectivity: ConnectivityObserver,
    private val getLatestHealthReport: GetLatestHealthReportUseCase,
    private val getHealthReportDetail: GetHealthReportDetailUseCase,
    private val generateHealthReport: GenerateHealthReportUseCase,
    private val downloadHealthReportPdf: DownloadHealthReportPdfUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: String = savedStateHandle["vehicleId"] ?: ""
    private val initialPlate: String? = savedStateHandle["plate"]

    private val _uiState = MutableStateFlow(AiHealthReportUiState(vehicleId = vehicleId, plate = initialPlate))
    val uiState: StateFlow<AiHealthReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val planAi = session?.plan?.aiDiagnosticsEnabled ?: true
                val canGen = session?.canUseAiDiagnostics() == true
                val canRd = session?.canReadAiReports() == true ||
                    (session?.has(Permission.HEALTH_REPORTS_READ) == true && planAi)
                _uiState.update {
                    it.copy(
                        planIncludesAi = planAi,
                        canGenerate = canGen,
                        canRead = canRd
                    )
                }
                if (planAi && canRd && _uiState.value.latestSummary == null && !_uiState.value.isLoading) {
                    loadLatest()
                }
            }
        }

        if (vehicleId.isNotBlank()) {
            viewModelScope.launch {
                observeVehicle(vehicleId).collect { vehicle ->
                    _uiState.update { it.copy(vehicle = vehicle) }
                }
            }
        }

        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun loadLatest() {
        val state = _uiState.value
        if (!state.planIncludesAi || !state.canRead || vehicleId.isBlank() || state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getLatestHealthReport(vehicleId)
                .onSuccess { latest ->
                    if (latest == null) {
                        _uiState.update { it.copy(isLoading = false, latestSummary = null, detail = null) }
                    } else {
                        val detail = getHealthReportDetail(latest.reportId).getOrNull()
                        _uiState.update {
                            it.copy(isLoading = false, latestSummary = latest, detail = detail)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun openGenerateSheet() {
        val state = _uiState.value
        if (!state.planIncludesAi || !state.canGenerate || state.isGenerating) return
        _uiState.update { it.copy(generateSheetOpen = true, error = null) }
    }

    fun dismissGenerateSheet() = _uiState.update {
        if (it.isGenerating) it else it.copy(generateSheetOpen = false)
    }

    fun selectDays(days: Int) = _uiState.update { it.copy(daysToAnalyze = days) }

    fun toggleIncludeResolvedDtcHistory() = _uiState.update {
        it.copy(includeResolvedDtcHistory = !it.includeResolvedDtcHistory)
    }

    fun confirmGenerate() {
        val state = _uiState.value
        if (!state.planIncludesAi || !state.canGenerate || !state.isOnline || state.isGenerating || vehicleId.isBlank()) return
        _uiState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            generateHealthReport(vehicleId, state.daysToAnalyze, state.includeResolvedDtcHistory)
                .onSuccess { report ->
                    val detail = getHealthReportDetail(report.reportId).getOrNull()
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generateSheetOpen = false,
                            latestSummary = report,
                            detail = detail
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isGenerating = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun downloadPdf(mode: PdfActionMode = PdfActionMode.OPEN) {
        val state = _uiState.value
        val reportId = state.detail?.reportId ?: state.latestSummary?.reportId ?: return
        if (!state.canRead || !state.isOnline || state.isDownloadingPdf) return
        _uiState.update { it.copy(isDownloadingPdf = true, pdfActionMode = mode, error = null) }
        viewModelScope.launch {
            downloadHealthReportPdf(reportId)
                .onSuccess { file ->
                    _uiState.update { it.copy(isDownloadingPdf = false, pendingPdfFile = file, pdfActionMode = mode) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isDownloadingPdf = false, error = e.toDiagnosticsUiText()) }
                }
        }
    }

    fun sharePdf() = downloadPdf(PdfActionMode.SHARE)

    fun onPdfHandled() = _uiState.update { it.copy(pendingPdfFile = null) }

    fun onPdfOpenFailed() = _uiState.update {
        it.copy(pendingPdfFile = null, error = UiText.Resource(R.string.reports_pdf_no_app))
    }
}
