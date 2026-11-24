package com.andeva.atelier.mobile.features.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.SearchCustomersUseCase
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reports.application.DownloadCashFlowPdfUseCase
import com.andeva.atelier.mobile.features.reports.application.DownloadFleetHealthPdfUseCase
import com.andeva.atelier.mobile.features.reports.application.GetCashFlowSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getCashFlow: GetCashFlowSummaryUseCase,
    private val downloadCashFlowPdf: DownloadCashFlowPdfUseCase,
    private val downloadFleetHealthPdf: DownloadFleetHealthPdfUseCase,
    private val searchCustomers: SearchCustomersUseCase,
    observeSession: ObserveSessionUseCase,
    connectivity: ConnectivityObserver,
    clock: Clock
) : ViewModel() {

    private val today = BusinessTime.limaDate(clock.instant())
    private val _uiState = MutableStateFlow(
        ReportsUiState(today = today, period = periodFor(PeriodChoice.ThisMonth, today))
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val wasAllowed = _uiState.value.canReadCashFlow
                val canRead = session?.has(Permission.CASHFLOW_EXPORT_PDF) == true
                val hasFleetPermission = session?.has(Permission.HEALTH_REPORTS_READ) == true
                val planAllows = session?.canReadAiReports() == true
                _uiState.update {
                    it.copy(
                        canReadCashFlow = canRead,
                        canDownloadFleet = planAllows && session?.has(Permission.CUSTOMERS_READ) == true,
                        fleetLockedByPlan = hasFleetPermission && !planAllows
                    )
                }
                if (canRead && !wasAllowed) load()
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val state = _uiState.value
                val retry = online && !state.isOnline && state.canReadCashFlow && state.summary == null
                _uiState.update { it.copy(isOnline = online) }
                if (retry) load()
            }
        }
    }

    fun load() {
        val period = _uiState.value.period
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            getCashFlow(period)
                .onSuccess { summary -> _uiState.update { it.copy(summary = summary, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(summary = null, isLoading = false, error = e.toUiText()) } }
        }
    }

    fun onChoice(choice: PeriodChoice) {
        if (choice == _uiState.value.choice && !_uiState.value.showCustomRangePicker) return
        _uiState.update {
            it.copy(
                choice = choice,
                period = periodFor(choice, today),
                summary = null,
                showCustomRangePicker = false
            )
        }
        load()
    }

    fun onOpenCustomRange() {
        _uiState.update { it.copy(showCustomRangePicker = true) }
    }

    fun onDismissCustomRange() {
        _uiState.update { it.copy(showCustomRangePicker = false) }
    }

    /**
     * Convierte los milisegundos epoch UTC de [androidx.compose.material3.DateRangePickerState] en [LocalDate] usando
     * estrictamente [ZoneOffset.UTC] para evitar el desfase de -1 día en `America/Lima`.
     */
    fun onCustomRangePicked(startMillisUtc: Long?, endMillisUtc: Long?) {
        val startMs = startMillisUtc ?: return
        val endMs = endMillisUtc ?: startMs
        val firstDate = Instant.ofEpochMilli(startMs).atZone(ZoneOffset.UTC).toLocalDate()
        val secondDate = Instant.ofEpochMilli(endMs).atZone(ZoneOffset.UTC).toLocalDate()
        val from = minOf(firstDate, secondDate)
        val to = maxOf(firstDate, secondDate)
        onChoice(PeriodChoice.Custom(from, to))
    }

    fun onDownloadCashFlow(mode: ReportPdfActionMode = ReportPdfActionMode.OPEN) {
        val state = _uiState.value
        if (!state.canReadCashFlow || !state.isOnline || state.busy != null) return
        _uiState.update {
            it.copy(
                busy = ReportDownload.CashFlow,
                pdfActionMode = mode,
                actionError = null
            )
        }
        viewModelScope.launch {
            downloadCashFlowPdf(state.period)
                .onSuccess { file -> _uiState.update { it.copy(pdf = file) } }
                .onFailure { e -> _uiState.update { it.copy(actionError = e.toUiText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    fun onOpenCashFlowPdf() = onDownloadCashFlow(ReportPdfActionMode.OPEN)

    fun onShareCashFlowPdf() = onDownloadCashFlow(ReportPdfActionMode.SHARE)

    fun onOpenFleet() {
        if (!_uiState.value.canDownloadFleet) return
        _uiState.update { it.copy(fleetSheet = FleetSheet(), actionError = null) }
    }

    fun onFleetActionMode(mode: ReportPdfActionMode) = updateFleet { it.copy(actionMode = mode) }

    fun onFleetQuery(value: String) = updateFleet { it.copy(query = value, error = null) }

    fun onFleetSearch() {
        val sheet = _uiState.value.fleetSheet ?: return
        val query = sheet.query.trim()
        if (query.isEmpty() || sheet.isSearching) return
        updateFleet { it.copy(isSearching = true, error = null) }
        viewModelScope.launch {
            searchCustomers(query)
                .onSuccess { customers -> updateFleet { it.copy(results = customers, hasSearched = true) } }
                .onFailure { e -> updateFleet { it.copy(results = emptyList(), error = e.toUiText(), hasSearched = true) } }
            updateFleet { it.copy(isSearching = false) }
        }
    }

    fun onFleetPick(customer: Customer, modeOverride: ReportPdfActionMode? = null) {
        val state = _uiState.value
        if (!state.canDownloadFleet || !state.isOnline || state.busy != null) return
        val mode = modeOverride ?: state.fleetSheet?.actionMode ?: ReportPdfActionMode.OPEN
        _uiState.update { it.copy(busy = ReportDownload.FleetHealth, pdfActionMode = mode) }
        updateFleet { it.copy(error = null) }
        viewModelScope.launch {
            downloadFleetHealthPdf(customer.id)
                .onSuccess { file -> _uiState.update { it.copy(pdf = file, fleetSheet = null) } }
                .onFailure { e -> updateFleet { it.copy(error = e.toUiText()) } }
            _uiState.update { it.copy(busy = null) }
        }
    }

    fun onDismissFleet() {
        if (_uiState.value.busy == ReportDownload.FleetHealth) return
        _uiState.update { it.copy(fleetSheet = null) }
    }

    private fun updateFleet(change: (FleetSheet) -> FleetSheet) = _uiState.update { state ->
        state.fleetSheet?.let { state.copy(fleetSheet = change(it)) } ?: state
    }

    fun onPdfHandled() = _uiState.update { it.copy(pdf = null) }

    fun onPdfOpenFailed() =
        _uiState.update { it.copy(pdf = null, actionError = UiText.Resource(R.string.reports_pdf_no_app)) }
}
